package uz.hesap.service.document.service.c2c;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.document.domain.document.CancelRequestEntity;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.CancelRequestStatus;
import uz.hesap.service.document.mapper.CancelRequestMapper;
import uz.hesap.service.document.model.request.CancelRequestCreateRequest;
import uz.hesap.service.document.model.response.CancelRequestResponse;
import uz.hesap.service.document.repository.CancelRequestRepository;
import uz.hesap.service.document.repository.PaymentScheduleRepository;
import uz.hesap.service.document.service.document.DocumentQueryHelper;
import uz.hesap.service.document.service.template.TemplateNotificationDispatcher;

// Shartnomani bekor qilish so'rovlari: yaratish / tasdiq (→ CANCELLED) / rad / bekor / ro'yxat.
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelRequestsService {

  private final CancelRequestRepository cancelRequestRepository;
  private final DocumentQueryHelper documentQueryHelper;
  private final uz.hesap.service.document.repository.DocumentRepository documentRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final CancelRequestMapper cancelRequestMapper;
  private final TemplateNotificationDispatcher notificationDispatcher;

  // ================ CREATE ================

  @Transactional
  public Mono<Void> create(UserPrincipal user, CancelRequestCreateRequest request) {
    UUID userId = user.user().id();
    String requesterIn = user.user().identifier();
    return documentQueryHelper
        .findAndValidateParty(request.contractId(), userId)
        .flatMap(
            doc -> {
              if (doc.getStatus() != DocumentStatus.ACTIVE) {
                return Mono.<CancelRequestEntity>error(
                    new InvalidOperationException(
                        "Faqat faol shartnomani bekor qilishni so'rash mumkin"));
              }
              return cancelRequestRepository
                  .findFirstByContractIdAndStatusAndDeletedFalse(
                      doc.getId(), CancelRequestStatus.PENDING)
                  .flatMap(
                      existing ->
                          Mono.<CancelRequestEntity>error(
                              new InvalidOperationException(
                                  "Bekor qilish so'rovi allaqachon mavjud")))
                  .switchIfEmpty(Mono.defer(() -> save(doc, requesterIn, request.reason())))
                  .delayUntil(saved -> notifyOtherParty(doc, userId, NotificationEvent.CANCEL_REQUEST));
            })
        .then();
  }

  private Mono<CancelRequestEntity> save(DocumentEntity doc, String requesterIn, String reason) {
    CancelRequestEntity entity = new CancelRequestEntity();
    entity.setContractId(doc.getId());
    entity.setBuyerIn(doc.getBuyerIn());
    entity.setSellerIn(doc.getSellerIn());
    entity.setRequesterIn(requesterIn);
    entity.setStatus(CancelRequestStatus.PENDING);
    entity.setReason(reason);
    return cancelRequestRepository.save(entity);
  }

  // ================ APPROVE (→ shartnoma CANCELLED) ================

  @Transactional
  public Mono<Void> approve(UserPrincipal user, UUID id) {
    UUID userId = user.user().id();
    return pending(id)
        .flatMap(
            cr ->
                documentQueryHelper
                    .findAndValidateParty(cr.getContractId(), userId)
                    .flatMap(
                        doc -> {
                          // So'rovni yaratgan taraf o'zi tasdiqlay olmaydi.
                          if (user.user().identifier() != null
                              && user.user().identifier().equals(cr.getRequesterIn())) {
                            return Mono.<Void>error(
                                new ForbiddenException(
                                    "So'rovni yaratuvchi o'zi tasdiqlay olmaydi"));
                          }
                          cr.setStatus(CancelRequestStatus.APPROVED);
                          doc.setStatus(DocumentStatus.CANCELLED);
                          return cancelRequestRepository
                              .save(cr)
                              .then(documentRepository.save(doc))
                              .then(
                                  paymentScheduleRepository.updateContractStatusByContractId(
                                      doc.getId(), DocumentStatus.CANCELLED.name()))
                              .then(notifyOtherParty(doc, userId, NotificationEvent.CANCEL_REQUEST));
                        }))
        .then();
  }

  // ================ REJECT ================

  @Transactional
  public Mono<Void> reject(UserPrincipal user, UUID id) {
    UUID userId = user.user().id();
    return pending(id)
        .flatMap(
            cr ->
                documentQueryHelper
                    .findAndValidateParty(cr.getContractId(), userId)
                    .flatMap(
                        doc -> {
                          if (user.user().identifier() != null
                              && user.user().identifier().equals(cr.getRequesterIn())) {
                            return Mono.<Void>error(
                                new ForbiddenException("So'rovni yaratuvchi o'zi rad eta olmaydi"));
                          }
                          cr.setStatus(CancelRequestStatus.REJECTED);
                          return cancelRequestRepository
                              .save(cr)
                              .then(notifyOtherParty(doc, userId, NotificationEvent.CANCEL_REQUEST));
                        }))
        .then();
  }

  // ================ CANCEL (so'rovchi o'z so'rovini bekor qiladi) ================

  @Transactional
  public Mono<Void> cancel(UUID id) {
    return pending(id)
        .flatMap(
            cr -> {
              cr.setStatus(CancelRequestStatus.CANCELLED);
              return cancelRequestRepository.save(cr);
            })
        .then();
  }

  // ================ LIST ================

  public Flux<CancelRequestResponse> getFiltered(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      List<CancelRequestStatus> statuses) {
    return cancelRequestRepository
        .findFiltered(buyerIn, sellerIn, fromIn, toIn, contractId, statuses)
        .map(cancelRequestMapper::toResponse);
  }

  // Shartnoma ichiga nest qilish uchun — bitta shartnomaning barcha so'rovlari.
  public Flux<CancelRequestResponse> getByContract(UUID contractId) {
    return cancelRequestRepository
        .findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(contractId)
        .map(cancelRequestMapper::toResponse);
  }

  // ================ HELPERS ================

  private Mono<CancelRequestEntity> pending(UUID id) {
    return cancelRequestRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Bekor qilish so'rovi topilmadi")))
        .flatMap(
            cr ->
                cr.getStatus() == CancelRequestStatus.PENDING
                    ? Mono.just(cr)
                    : Mono.error(
                        new InvalidOperationException("So'rov PENDING holatida emas")));
  }

  // qarshi tarafga notification (recipient UUID PINFL'dan tiklanadi)
  private Mono<Void> notifyOtherParty(DocumentEntity doc, UUID actorUserId, NotificationEvent event) {
    return documentQueryHelper
        .resolveOppositePartyUserId(doc, actorUserId)
        .flatMap(
            recipientId ->
                notificationDispatcher.dispatch(
                    doc.getTemplateId(), event, recipientId, doc.getNumber(), null, doc.getId()))
        .onErrorResume(
            e -> {
              log.error("Cancel request notification xatolik, doc [{}]: {}", doc.getId(), e.getMessage());
              return Mono.empty();
            })
        .then();
  }
}
