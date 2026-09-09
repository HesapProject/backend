package uz.hesap.service.document.service.payment;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.enums.RequestType;
import uz.hesap.service.document.domain.payment.DelayRequestEntity;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;
import uz.hesap.service.document.model.request.*;
import uz.hesap.service.document.repository.*;
import uz.hesap.service.document.service.document.DocumentQueryHelper;
import uz.hesap.service.jms.JmsPublisher;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentScheduleCommandService {

  private final PaymentScheduleRepository paymentScheduleRepository;
  private final PaymentScheduleRequestRepository paymentScheduleRequestRepository;
  private final DelayRequestRepository delayRequestRepository;
  private final PaidScheduleRepository paidScheduleRepository;
  private final DocumentRepository documentRepository;
  private final uz.hesap.service.document.service.c2c.ContractCompletionService
      contractCompletionService;
  private final JmsPublisher jmsPublisher;
  private final DocumentQueryHelper documentQueryHelper;
  private final uz.hesap.service.document.service.document.ContractTimelinePublisher timelinePublisher;
  private final uz.hesap.service.document.service.document.WebhookEventPublisher
      webhookEventPublisher;
  private final uz.hesap.service.document.service.template.TemplateNotificationDispatcher
      notificationDispatcher;

  // ================ PAYMENT SCHEDULE CRUD ================

  // to'lov jadvalini yaratish/yangilash/o'chirish (toggle)
  @Transactional
  public Mono<Void> togglePaymentSchedule(UUID documentId, List<PaymentScheduleRequest> requests) {
    if (requests == null || requests.isEmpty()) return Mono.empty();

    // documentdan buyer/seller IDlarni olish
    return documentQueryHelper
        .findOrThrow(documentId)
        .flatMap(
            doc ->
                paymentScheduleRepository
                    .findAllByContractIdAndDeletedFalse(documentId)
                    .collectList()
                    .flatMap(existing -> applyToggleChanges(existing, requests, doc)));
  }

  // to'lov jadvalini o'chirish (soft delete)
  @Transactional
  public Mono<Void> deletePaymentSchedule(UUID paymentId) {
    return paymentScheduleRepository
        .findByIdAndDeletedFalse(paymentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment schedule not found")))
        .flatMap(this::softDeletePaymentSchedule);
  }

  // ================ PAYMENT REQUEST (buyer → seller) ================

  // buyer to'lov so'rovi yaratadi
  @Transactional
  public Mono<Void> createPaymentRequest(
      UUID documentId, UserResponse user, List<PaymentScheduleRequestRequest> requests) {
    if (requests == null || requests.isEmpty()) return Mono.empty();

    return documentQueryHelper
        .findOrThrow(documentId)
        .flatMap(doc -> savePaymentRequests(doc, user, requests))
        .then();
  }

  // to'lov so'rovini tasdiqlash → PaidSchedule ga saqlash
  @Transactional
  public Mono<Void> approvePaymentRequest(UUID requestId, UserPrincipal userPrincipal) {
    return paymentScheduleRequestRepository
        .findByIdAndDeletedFalse(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment request not found")))
        .flatMap(request -> processRequestApproval(request));
  }

  // to'lov so'rovini rad etish
  @Transactional
  public Mono<Void> rejectPaymentRequest(UUID requestId) {
    return paymentScheduleRequestRepository
        .findByIdAndDeletedFalse(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment request not found")))
        .flatMap(this::rejectRequest);
  }

  // to'lov so'rovini o'chirish
  @Transactional
  public Mono<Void> deletePaymentRequest(UUID requestId) {
    return paymentScheduleRequestRepository
        .findById(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment request not found")))
        .flatMap(this::softDeleteRequest);
  }

  // paymentScheduleId orqali to'lov so'rovi yuborish (documentId schedule'dan olinadi).
  public Mono<Void> createPaymentRequest(UserResponse user, PaymentScheduleRequestRequest request) {
    return paymentScheduleRepository
        .findByIdAndDeletedFalse(request.paymentScheduleId())
        .switchIfEmpty(Mono.error(new NotFoundException("Payment schedule not found")))
        .flatMap(schedule -> createPaymentRequest(schedule.getContractId(), user, List.of(request)));
  }

  // to'lov so'rovini bekor qilish (yuboruvchi) — CANCELLED.
  @Transactional
  public Mono<Void> cancelPaymentRequest(UUID requestId) {
    return paymentScheduleRequestRepository
        .findByIdAndDeletedFalse(requestId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment request not found")))
        .flatMap(
            request -> {
              request.setStatus(PaymentScheduleStatus.CANCELLED);
              return paymentScheduleRequestRepository
                  .save(request)
                  .doOnSuccess(
                      saved ->
                          timelinePublisher.publish(
                              request.getContractId(),
                              "PAYMENT_REQUEST_REJECTED",
                              "BUYER",
                              request.getBuyerIn(),
                              request.getAmount(),
                              cur(request.getCurrency())))
                  .then();
            });
  }

  // ================ DELAY PAYMENT ================

  // kechiktirish so'rovi (buyer)
  public Mono<Void> delayPayment(
      UserPrincipal userPrincipal, UUID paymentId, DelayPaymentRequest request) {
    return paymentScheduleRepository
        .findByIdAndDeletedFalse(paymentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment schedule not found")))
        .flatMap(p -> createDelayRequest(p, userPrincipal, request));
  }

  // Kechiktirish so'rovi (body'dagi paymentScheduleId + sana bo'yicha) — /delay-requests POST.
  public Mono<Void> createDelayRequest(
      UserPrincipal userPrincipal,
      uz.hesap.service.document.model.request.DelayRequestCreateRequest request) {
    return delayPayment(
        userPrincipal, request.paymentScheduleId(), new DelayPaymentRequest(request.date(), request.note()));
  }

  // kechiktirishni tasdiqlash/rad etish (seller)
  @Transactional
  public Mono<Void> updateDelayPayment(
      UserPrincipal userPrincipal, UUID delayId, DelayPaymentApproveRequest request) {
    return delayRequestRepository
        .findByIdAndDeletedFalse(delayId)
        .switchIfEmpty(Mono.error(new NotFoundException("Delay request not found")))
        .flatMap(delay -> processDelayApproval(delay, request));
  }

  // Kechiktirish so'rovini qabul qilish (APPROVED) — /delay-requests/approve.
  public Mono<Void> approveDelayRequest(UserPrincipal userPrincipal, UUID delayId) {
    return updateDelayPayment(
        userPrincipal, delayId, new DelayPaymentApproveRequest(PaymentScheduleStatus.APPROVED));
  }

  // Kechiktirish so'rovini rad etish (CANCELLED) — /delay-requests/reject.
  public Mono<Void> rejectDelayRequest(UserPrincipal userPrincipal, UUID delayId) {
    return updateDelayPayment(
        userPrincipal, delayId, new DelayPaymentApproveRequest(PaymentScheduleStatus.CANCELLED));
  }

  // Kechiktirish so'rovini bekor qilish (yuboruvchi) — /delay-requests/cancel. Status CANCELLED.
  @Transactional
  public Mono<Void> cancelDelayRequest(UUID delayId) {
    return delayRequestRepository
        .findByIdAndDeletedFalse(delayId)
        .switchIfEmpty(Mono.error(new NotFoundException("Delay request not found")))
        .flatMap(
            delay -> {
              delay.setStatus(PaymentScheduleStatus.CANCELLED);
              return delayRequestRepository.save(delay).then();
            });
  }

  // ================ PAID APPROVE ================

  // seller to'lovni tasdiqlash/rad etish
  public Mono<Void> paidApprove(
      UserPrincipal userPrincipal, UUID paidId, UpdateStatusRequest request) {
    return paidScheduleRepository
        .findByIdAndDeletedFalse(paidId)
        .switchIfEmpty(Mono.error(new NotFoundException("Paid schedule not found")))
        .flatMap(paid -> processPaidApproval(paid, userPrincipal, request));
  }

  // ================ PRIVATE HELPERS ================

  // toggle: mavjudlarni soft-delete + yangilarni upsert — buyer/seller IDlar documentdan olinadi
  private Mono<Void> applyToggleChanges(
      List<PaymentEntity> existing,
      List<PaymentScheduleRequest> requests,
      DocumentEntity doc) {
    Map<UUID, PaymentEntity> existingById =
        existing.stream()
            .filter(e -> e.getId() != null)
            .collect(Collectors.toMap(PaymentEntity::getId, Function.identity()));

    Set<UUID> requestIds =
        requests.stream()
            .map(PaymentScheduleRequest::id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // requestda yo'q bo'lganlarni soft-delete
    List<PaymentEntity> toDelete =
        existing.stream()
            .filter(e -> e.getId() != null && !requestIds.contains(e.getId()))
            .peek(e -> e.setDeleted(Boolean.TRUE))
            .toList();

    // yangi/mavjudlarni upsert — buyer/seller documentdan olinadi
    List<PaymentEntity> toUpsert =
        requests.stream().map(req -> buildOrUpdateSchedule(req, existingById, doc)).toList();

    return paymentScheduleRepository
        .saveAll(Flux.fromIterable(toDelete))
        .thenMany(paymentScheduleRepository.saveAll(Flux.fromIterable(toUpsert)))
        .then();
  }

  // schedule entity yaratish yoki yangilash — buyer/seller IDlar DocumentEntity dan
  private PaymentEntity buildOrUpdateSchedule(
      PaymentScheduleRequest req, Map<UUID, PaymentEntity> existing, DocumentEntity doc) {
    PaymentEntity e;
    if (req.id() != null && existing.containsKey(req.id())) {
      // mavjud entity — UPDATE
      e = existing.get(req.id());
    } else {
      // yangi entity — ID null qoldiriladi (R2DBC INSERT qiladi)
      e = new PaymentEntity();
    }
    // buyer/seller PINFL va holat documentdan
    e.setBuyerIn(doc.getBuyerIn());
    e.setSellerIn(doc.getSellerIn());
    e.setContractId(doc.getId());
    e.setContractStatus(doc.getStatus());
    e.setStatus(req.status() != null ? req.status() : PaymentScheduleStatus.PENDING);
    e.setTotalAmount(req.amount());
    e.setContractPaymentDate(req.paymentDate());
    e.setDeleted(Boolean.FALSE);
    return e;
  }

  // soft delete qilish (agar to'langan bo'lsa — xatolik)
  private Mono<Void> softDeletePaymentSchedule(PaymentEntity p) {
    if (p.getStatus() == PaymentScheduleStatus.PAID) {
      return Mono.error(new InvalidOperationException("Payment schedule already paid"));
    }
    p.setDeleted(Boolean.TRUE);
    return paymentScheduleRepository.save(p).then();
  }

  // payment requestlarni saqlash — buyer/seller IDlar documentdan olinadi
  private Mono<Void> savePaymentRequests(
      DocumentEntity doc, UserResponse user, List<PaymentScheduleRequestRequest> requests) {
    List<PaymentScheduleRequestEntity> entities =
        requests.stream().map(r -> buildRequestEntity(r, doc, user)).toList();
    // Haqdor (seller) o'zi yaratsa — bu "to'landi" belgilash; so'rov darhol
    // tasdiqlanadi (PaidSchedule yaratiladi). Qarzdor (buyer) yaratsa — PENDING qoladi.
    final boolean sellerInitiated =
        doc.getSellerIn() != null && doc.getSellerIn().equals(user.identifier());

    // Qarzdor yuborganda: bitta to'lovga bitta faol (PENDING) so'rov.
    Mono<Void> pendingGate =
        sellerInitiated
            ? Mono.empty()
            : reactor.core.publisher.Flux.fromIterable(entities)
                .map(PaymentScheduleRequestEntity::getPaymentId)
                .distinct()
                .filterWhen(
                    pid ->
                        paymentScheduleRequestRepository
                            .existsByPaymentIdAndStatusAndDeletedFalse(
                                pid, PaymentScheduleStatus.PENDING))
                .hasElements()
                .flatMap(
                    hasPending ->
                        Boolean.TRUE.equals(hasPending)
                            ? Mono.error(
                                new InvalidOperationException(
                                    "Bu to'lov bo'yicha faol so'rov allaqachon mavjud"))
                            : Mono.empty());

    return pendingGate
        .then(paymentScheduleRequestRepository.saveAll(entities).collectList())
        .doOnSuccess(
            saved -> {
              if (!sellerInitiated) {
                entities.forEach(
                    e ->
                        timelinePublisher.publish(
                            e.getContractId(),
                            "PAYMENT_REQUEST_SENT",
                            "BUYER",
                            e.getBuyerIn(),
                            e.getAmount(),
                            cur(e.getCurrency())));
              }
            })
        .delayUntil(saved -> notifyPaymentRequest(doc, user))
        .flatMap(
            saved ->
                sellerInitiated
                    ? Flux.fromIterable(saved).concatMap(this::processRequestApproval).then()
                    : Mono.<Void>empty())
        .then();
  }

  // request entity yaratish — buyer/seller documentdan, userId tokendan
  private PaymentScheduleRequestEntity buildRequestEntity(
      PaymentScheduleRequestRequest req, DocumentEntity doc, UserResponse user) {
    PaymentScheduleRequestEntity e = new PaymentScheduleRequestEntity();
    e.setBuyerIn(doc.getBuyerIn());
    e.setSellerIn(doc.getSellerIn());
    e.setCreatorIn(user.identifier()); // so'rovni yaratgan taraf
    e.setContractId(doc.getId());
    e.setPaymentId(req.paymentScheduleId());
    e.setAmount(req.amount());
    e.setCurrency(doc.getCurrency());
    e.setPaymentDate(req.paymentDate());
    e.setNote(req.note());
    e.setImage(req.image());
    e.setStatus(PaymentScheduleStatus.PENDING);
    return e;
  }

  // request tasdiqlash → PaidSchedule yaratish
  private Mono<Void> processRequestApproval(PaymentScheduleRequestEntity request) {
    if (request.getStatus() == PaymentScheduleStatus.APPROVED) {
      return Mono.error(new InvalidOperationException("Already approved"));
    }

    request.setStatus(PaymentScheduleStatus.APPROVED);
    timelinePublisher.publish(
        request.getContractId(),
        "PAYMENT_REQUEST_APPROVED",
        "SELLER",
        request.getSellerIn(),
        request.getAmount(),
        cur(request.getCurrency()));
    // OpenAPI webhook — hamkor tizimi to'lov qabul qilinganini bilsin.
    webhookEventPublisher.publishPayment(
        request.getContractId(),
        request.getBuyerIn(),
        request.getSellerIn(),
        request.getSellerIn(),
        request.getAmount(),
        cur(request.getCurrency()));

    // paymentId bor bo'lsa — PaidSchedule ga saqlash
    if (request.getPaymentId() != null) {
      return approveAndCreatePaid(request);
    }

    // paymentScheduleId yo'q — yangi PaymentSchedule yaratish
    return approveAndCreateSchedule(request);
  }

  // mavjud schedule ga paid qo'shish
  private Mono<Void> approveAndCreatePaid(PaymentScheduleRequestEntity request) {
    return paymentScheduleRepository
        .findByIdAndDeletedFalse(request.getPaymentId())
        .switchIfEmpty(Mono.error(new NotFoundException("Payment schedule not found")))
        .flatMap(
            schedule -> {
              // to'langan miqdorni tekshirish
              return paidScheduleRepository
                  .findAllByPaymentScheduleIdAndDeletedFalse(schedule.getId())
                  .map(PaidScheduleEntity::getAmount)
                  .reduce(0.0, Double::sum)
                  .flatMap(
                      totalPaid -> {
                        double newTotal = totalPaid + request.getAmount();
                        if (newTotal > schedule.getTotalAmount()) {
                          return Mono.error(
                              new InvalidOperationException(
                                  "Total paid amount exceeds scheduled amount"));
                        }

                        // PaidSchedule yaratish
                        PaidScheduleEntity paid = buildPaidFromRequest(request, schedule);

                        // agar to'liq to'langan bo'lsa — schedule ni PAID qilish
                        Mono<PaymentEntity> updateSchedule;
                        schedule.setPaidAmount(newTotal);
                        if (newTotal >= schedule.getTotalAmount()) {
                          schedule.setStatus(PaymentScheduleStatus.PAID);
                          // To'liq to'langan vaqt.
                          schedule.setPaidAt(Instant.now());
                        }
                        updateSchedule = paymentScheduleRepository.save(schedule);

                        return paymentScheduleRequestRepository
                            .save(request)
                            .then(paidScheduleRepository.save(paid))
                            .then(updateSchedule)
                            // to'lov amalga oshganligi notification (seller ga)
                            .delayUntil(saved -> notifyPaymentPaid(schedule))
                            // request tasdiqlanganligi notification (buyer ga)
                            .delayUntil(saved -> notifyRequestApproved(schedule))
                            // to'lov to'liq bo'lsa — shartnoma yakunlanishini tekshirish
                            // (barcha to'lov + barcha topshirish tugagan bo'lsa COMPLETED).
                            .then(
                                contractCompletionService.completeIfDone(
                                    schedule.getContractId()))
                            .then();
                      });
            });
  }

  // yangi schedule yaratish (paymentScheduleId yo'q — yangi payment qo'shish)
  private Mono<Void> approveAndCreateSchedule(PaymentScheduleRequestEntity request) {
    PaymentEntity schedule = new PaymentEntity();
    schedule.setStatus(PaymentScheduleStatus.PENDING);
    schedule.setBuyerIn(request.getBuyerIn());
    schedule.setSellerIn(request.getSellerIn());
    schedule.setContractId(request.getContractId());
    schedule.setTotalAmount(request.getAmount());
    schedule.setContractPaymentDate(request.getPaymentDate());

    request.setStatus(PaymentScheduleStatus.APPROVED);

    return Mono.zip(
            paymentScheduleRepository.save(schedule),
            paymentScheduleRequestRepository.save(request))
        .then();
  }

  // PaidSchedule entity yaratish
  private PaidScheduleEntity buildPaidFromRequest(
      PaymentScheduleRequestEntity request, PaymentEntity schedule) {
    PaidScheduleEntity paid = new PaidScheduleEntity();
    paid.setBuyerIn(schedule.getBuyerIn());
    paid.setSellerIn(schedule.getSellerIn());
    paid.setDocumentId(schedule.getContractId());
    paid.setPaymentScheduleId(schedule.getId());
    paid.setPaymentDate(Instant.now());
    paid.setAmount(request.getAmount());
    paid.setStatus(PaymentScheduleStatus.PENDING);
    return paid;
  }

  // request rad etish
  private Mono<Void> rejectRequest(PaymentScheduleRequestEntity request) {
    if (request.getStatus() == PaymentScheduleStatus.APPROVED) {
      return Mono.error(new InvalidOperationException("Already approved, cannot reject"));
    }
    request.setStatus(PaymentScheduleStatus.CANCELLED);
    return paymentScheduleRequestRepository
        .save(request)
        .doOnSuccess(
            saved ->
                timelinePublisher.publish(
                    request.getContractId(),
                    "PAYMENT_REQUEST_REJECTED",
                    "SELLER",
                    request.getSellerIn(),
                    request.getAmount(),
                    cur(request.getCurrency())))
        // request rad etilganligi notification (buyer ga)
        .delayUntil(saved -> notifyRequestRejected(request))
        .then();
  }

  // request soft-delete
  private Mono<Void> softDeleteRequest(PaymentScheduleRequestEntity p) {
    if (p.getStatus() == PaymentScheduleStatus.APPROVED) {
      return Mono.error(new InvalidOperationException("Payment request already approved"));
    }
    p.setDeleted(Boolean.TRUE);
    return paymentScheduleRequestRepository.save(p).then();
  }

  // kechiktirish so'rovi yaratish
  private Mono<Void> createDelayRequest(
      PaymentEntity p, UserPrincipal userPrincipal, DelayPaymentRequest request) {
    if (p.getStatus() == PaymentScheduleStatus.PAID) {
      return Mono.error(new InvalidOperationException("Already paid"));
    }
    if (request.date() == null) {
      return Mono.error(new InvalidOperationException("Date is required"));
    }
    // Amaldagi muddat: allaqachon kechiktirilgan bo'lsa o'sha sana, aks holda asl sana.
    // KUN bo'yicha solishtiriladi (vaqt komponenti va timezone xalal bermasin);
    // eski (migratsiya) yozuvlarda sana NULL bo'lishi mumkin — unda tekshirilmaydi.
    java.time.Instant current =
        p.getChangedPaymentDate() != null ? p.getChangedPaymentDate() : p.getContractPaymentDate();
    if (current != null
        && request
            .date()
            .truncatedTo(java.time.temporal.ChronoUnit.DAYS)
            .isBefore(current.truncatedTo(java.time.temporal.ChronoUnit.DAYS))) {
      return Mono.error(new InvalidOperationException("Yangi sana amaldagi to'lov sanasidan oldin bo'lishi mumkin emas"));
    }

    // buyer ekanligini tekshirish
    if (!isCurrentUserBuyer(userPrincipal, p)) {
      return Mono.error(new ForbiddenException("Only buyer can request delay"));
    }

    return delayRequestRepository
        .existsByPaymentIdAndStatusAndDeletedFalse(p.getId(), PaymentScheduleStatus.PENDING)
        .flatMap(
            hasPending -> {
              // Bitta to'lovga bitta faol so'rov — bekor/rad etilgach yana yuborsa bo'ladi.
              if (Boolean.TRUE.equals(hasPending)) {
                return Mono.error(
                    new InvalidOperationException(
                        "Bu to'lov bo'yicha faol kechiktirish so'rovi allaqachon mavjud"));
              }
              return saveDelayRequest(p, request);
            });
  }

  private Mono<Void> saveDelayRequest(PaymentEntity p, DelayPaymentRequest request) {
    DelayRequestEntity entity = new DelayRequestEntity();
    entity.setBuyerIn(p.getBuyerIn());
    entity.setSellerIn(p.getSellerIn());
    entity.setContractId(p.getContractId());
    entity.setPaymentId(p.getId());
    entity.setStatus(PaymentScheduleStatus.PENDING);
    entity.setPaymentDate(request.date());
    entity.setNote(request.note());
    entity.setAmount(p.getTotalAmount());

    return delayRequestRepository
        .save(entity)
        .doOnSuccess(
            saved ->
                timelinePublisher.publish(
                    entity.getContractId(),
                    "DELAY_REQUEST_SENT",
                    "BUYER",
                    entity.getBuyerIn(),
                    entity.getAmount(),
                    cur(entity.getCurrency())))
        .delayUntil(saved -> notifyDelayRequest(p))
        .then();
  }

  // kechiktirishni tasdiqlash/rad etish
  private Mono<Void> processDelayApproval(
      DelayRequestEntity delay, DelayPaymentApproveRequest request) {
    if (delay.getStatus() == PaymentScheduleStatus.APPROVED) {
      return Mono.error(new InvalidOperationException("Already approved"));
    }

    delay.setStatus(request.status());

    return delayRequestRepository
        .save(delay)
        .doOnSuccess(
            saved ->
                timelinePublisher.publish(
                    delay.getContractId(),
                    request.status() == PaymentScheduleStatus.APPROVED
                        ? "DELAY_REQUEST_APPROVED"
                        : "DELAY_REQUEST_REJECTED",
                    "SELLER",
                    delay.getSellerIn(),
                    delay.getAmount(),
                    cur(delay.getCurrency())))
        .then(updateScheduleAfterDelay(delay, request));
  }

  // kechiktirish tasdiqlanganda sana yangilash
  private Mono<Void> updateScheduleAfterDelay(
      DelayRequestEntity delay, DelayPaymentApproveRequest request) {
    return paymentScheduleRepository
        .findByIdAndDeletedFalse(delay.getPaymentId())
        .switchIfEmpty(Mono.error(new NotFoundException("Payment schedule not found")))
        .flatMap(
            payment -> {
              if (payment.getStatus() == PaymentScheduleStatus.PAID) {
                return Mono.error(new InvalidOperationException("Already paid"));
              }
              if (request.status() == PaymentScheduleStatus.APPROVED) {
                payment.setChangedPaymentDate(delay.getPaymentDate());
              }
              return paymentScheduleRepository
                  .save(payment)
                  .delayUntil(saved -> notifyDelayResponse(payment, request))
                  .then();
            });
  }

  // seller to'lovni approve/reject qilish
  private Mono<Void> processPaidApproval(
      PaidScheduleEntity paid, UserPrincipal userPrincipal, UpdateStatusRequest request) {
    if (!isCurrentUserSeller(userPrincipal, paid)) {
      return Mono.error(new ForbiddenException("Only seller can approve payment"));
    }
    if (paid.getStatus() != PaymentScheduleStatus.PENDING) {
      return Mono.error(new InvalidOperationException("Already processed"));
    }

    paid.setStatus(request.status());
    return paidScheduleRepository
        .save(paid)
        .doOnSuccess(
            saved -> {
              if (request.status() == PaymentScheduleStatus.PAID) {
                timelinePublisher.publish(
                    paid.getDocumentId(),
                    "PAYMENT_ACCEPTED",
                    "SELLER",
                    paid.getSellerIn(),
                    paid.getAmount(),
                    cur(paid.getCurrency()));
              }
            })
        .delayUntil(saved -> notifyPaidApproval(paid, request))
        .then();
  }

  // Currency enum → String (timeline event uchun).
  private static String cur(uz.hesap.service.document.domain.enums.Currency c) {
    return c != null ? c.name() : null;
  }

  // ================ AUTHORIZATION HELPERS ================

  // buyer ekanligini tekshirish — PINFL (identifier) bo'yicha
  private boolean isCurrentUserBuyer(UserPrincipal user, PaymentEntity p) {
    return user.user().identifier() != null && user.user().identifier().equals(p.getBuyerIn());
  }

  // seller ekanligini tekshirish — PINFL (identifier) bo'yicha
  private boolean isCurrentUserSeller(UserPrincipal user, PaidScheduleEntity p) {
    return user.user().identifier() != null && user.user().identifier().equals(p.getSellerIn());
  }

  // ================ NOTIFICATION HELPERS ================

  // to'lov so'rovi notification — buyer yuborganda SELLER'ga boradi.
  // (Seller o'zi "to'landi" deb belgilasa avto-approve bo'ladi — unda alohida
  // paymentPaid/requestApproved notificationlar ketadi, bu yerda jo'natilmaydi.)
  private Mono<Void> notifyPaymentRequest(DocumentEntity doc, UserResponse user) {
    if (user.in() != null && user.in().equals(doc.getSellerIn())) return Mono.empty();
    return documentQueryHelper
        .resolveUserIdByIn(doc.getSellerIn())
        .flatMap(
            recipientId ->
                notificationDispatcher.dispatch(
                    doc.getTemplateId(),
                    NotificationEvent.PAYMENT_REQUEST,
                    recipientId,
                    doc.getNumber(),
                    null,
                    doc.getId()));
  }

  // to'lov amalga oshganligi notification (seller ga — recipient UUID shartnomadan)
  private Mono<Void> notifyPaymentPaid(PaymentEntity schedule) {
    return getDocument(schedule.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getSellerIn())
                    .flatMap(
                        recipientId ->
                            sendNotification(
                                FirebaseNotificationReply.paymentPaid(
                                    doc.getId(), recipientId, doc.getNumber()))));
  }

  // kechiktirish so'rovi notification (seller ga)
  private Mono<Void> notifyDelayRequest(PaymentEntity p) {
    return getDocument(p.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getSellerIn())
                    .flatMap(
                        recipientId ->
                            notificationDispatcher.dispatch(
                                doc.getTemplateId(),
                                NotificationEvent.PAYMENT_DELAY,
                                recipientId,
                                doc.getNumber(),
                                null,
                                doc.getId())));
  }

  // kechiktirish javobi notification (buyer ga)
  private Mono<Void> notifyDelayResponse(
      PaymentEntity payment, DelayPaymentApproveRequest request) {
    return getDocument(payment.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getBuyerIn())
                    .flatMap(
                        recipientId -> {
                          if (request.status() == PaymentScheduleStatus.APPROVED) {
                            return sendNotification(
                                FirebaseNotificationReply.paymentDelayApproved(
                                    doc.getId(), recipientId, doc.getNumber()));
                          }
                          return sendNotification(
                              FirebaseNotificationReply.paymentDelayRejected(
                                  doc.getId(), recipientId, doc.getNumber()));
                        }));
  }

  // to'lov tasdiqlash notification (buyer ga)
  private Mono<Void> notifyPaidApproval(PaidScheduleEntity paid, UpdateStatusRequest request) {
    return getDocument(paid.getDocumentId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getBuyerIn())
                    .flatMap(
                        recipientId -> {
                          if (request.status() == PaymentScheduleStatus.PAID) {
                            return sendNotification(
                                FirebaseNotificationReply.paymentPaidApproved(
                                    doc.getId(), recipientId, doc.getNumber()));
                          }
                          return sendNotification(
                              FirebaseNotificationReply.paymentPaidRejected(
                                  doc.getId(), recipientId, doc.getNumber()));
                        }));
  }

  // request tasdiqlanganligi notification — buyer ga
  private Mono<Void> notifyRequestApproved(PaymentEntity schedule) {
    return getDocument(schedule.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getBuyerIn())
                    .flatMap(
                        recipientId ->
                            sendNotification(
                                FirebaseNotificationReply.paymentRequestApproved(
                                    doc.getId(), recipientId, doc.getNumber()))));
  }

  // request rad etilganligi notification — buyer ga (recipient UUID shartnomadan)
  private Mono<Void> notifyRequestRejected(PaymentScheduleRequestEntity request) {
    return getDocument(request.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveUserIdByIn(doc.getBuyerIn())
                    .flatMap(
                        recipientId ->
                            sendNotification(
                                FirebaseNotificationReply.paymentRequestRejected(
                                    doc.getId(), recipientId, doc.getNumber()))));
  }

  // document raqamini olish
  private Mono<String> getDocNumber(UUID documentId) {
    if (documentId == null) return Mono.just("—");
    return documentRepository
        .findByIdAndDeletedFalse(documentId)
        .map(DocumentEntity::getNumber)
        .defaultIfEmpty("—");
  }

  // Notification recipient UUID'sini shartnomadan olish uchun to'liq document.
  private Mono<DocumentEntity> getDocument(UUID documentId) {
    if (documentId == null) return Mono.empty();
    return documentRepository.findByIdAndDeletedFalse(documentId);
  }

  // RabbitMQ orqali notification yuborish
  private Mono<Void> sendNotification(FirebaseNotificationReply notification) {
    return jmsPublisher
        .publish(notification)
        .onErrorResume(
            e -> {
              log.error("Failed to send notification: {}", e.getMessage());
              return Mono.empty();
            });
  }
}
