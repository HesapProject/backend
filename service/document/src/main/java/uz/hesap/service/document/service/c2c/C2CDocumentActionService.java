package uz.hesap.service.document.service.c2c;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.DocumentSignatureEntity;
import uz.hesap.service.document.domain.document.DocumentWitnessEntity;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.common.util.enums.WebhookEventType;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;
import uz.hesap.service.document.domain.enums.SignatureActionType;
import uz.hesap.service.document.model.enums.ActionType;
import uz.hesap.service.document.domain.enums.VerificationType;
import uz.hesap.service.document.service.document.ContractTimelinePublisher;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.repository.DocumentSignatureRepository;
import uz.hesap.service.document.repository.DocumentWitnessRepository;
import uz.hesap.service.document.repository.WitnessRequestRepository;
import uz.hesap.service.document.repository.PaymentScheduleRepository;
import uz.hesap.service.document.service.document.DocumentQueryHelper;
import uz.hesap.service.document.webclient.IntegrationServiceClient;
import uz.hesap.service.document.webclient.UserServiceClient;
import uz.hesap.service.jms.JmsPublisher;

@Service
@RequiredArgsConstructor
@Log4j2
public class C2CDocumentActionService {
  // MyID yuz mosligi minimal bali (shu balldan past bo'lsa imzolashga ruxsat yo'q).
  private static final double MYID_MIN_COMPARISON = 0.6;

  private final DocumentRepository documentRepository;
  private final DocumentSignatureRepository documentSignatureRepository;
  private final DocumentWitnessRepository documentWitnessRepository;
  private final WitnessRequestRepository witnessRequestRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final VerificationService verificationService;
  private final UserServiceClient userServiceClient;
  private final IntegrationServiceClient integrationServiceClient;
  private final JmsPublisher jmsPublisher;
  private final DocumentQueryHelper documentQueryHelper;
  private final uz.hesap.service.document.repository.TemplateRepository templateRepository;
  private final uz.hesap.service.document.service.document.ContractTimelinePublisher
      timelinePublisher;
  private final uz.hesap.service.document.service.document.WebhookEventPublisher
      webhookEventPublisher;
  private final uz.hesap.service.document.service.template.TemplateNotificationDispatcher
      notificationDispatcher;

  public Mono<Void> sendVerificationSms(UUID documentId, ActionType action, UserResponse user) {
    // Taraf imzosi: avval barcha guvohlar imzolagan bo'lishi shart — OTP ham yuborilmaydi.
    Mono<Void> gate =
        action == ActionType.PARTY_ACCEPT ? ensureWitnessesSigned(documentId) : Mono.empty();
    return gate.then(
        findDocumentOrWitness(documentId, action, user.id())
            .flatMap(
                docId -> verificationService.sendOtp(user.id(), documentId, action, user.phone())));
  }

  // Shartnomada PENDING guvohlik so'rovi qolmaganini tekshiradi — guvohlar
  // imzolamasdan taraflar imzolay olmaydi.
  private Mono<Void> ensureWitnessesSigned(UUID documentId) {
    return witnessRequestRepository
        .findAllByContractId(documentId)
        .filter(w -> w.getStatus() == DocumentWitnessStatus.PENDING)
        .hasElements()
        .flatMap(
            hasPending ->
                Boolean.TRUE.equals(hasPending)
                    ? Mono.error(
                        new BadRequestException(
                            uz.hesap.service.common.exception.handler.ErrorCode
                                .WITNESS_SIGN_REQUIRED,
                            "Avval barcha guvohlar imzolashi kerak"))
                    : Mono.empty());
  }

  // ---------- Guvoh (witness) qabul: SMS yoki MyID ----------

  /** SMS OTP bilan guvoh qabul qiladi. */
  @Transactional
  public Mono<Void> acceptWitness(final UUID docId, final UUID witnessId, final Integer otpCode) {
    return witnessRequestRepository
        .findByContractIdAndWitnessId(docId, witnessId)
        .switchIfEmpty(Mono.error(new NotFoundException("Witness not found for this document")))
        .flatMap(
            witness ->
                verificationService
                    .verifyOtp(witnessId, docId, ActionType.WITNESS_ACCEPT, otpCode)
                    .then(applyWitnessAccept(witness, docId, witnessId)));
  }

  /** MyID (yuz) bilan guvoh qabul qiladi. */
  @Transactional
  public Mono<Void> signWitnessByMyId(
      final UUID docId, final UUID witnessId, final String code, final String platform) {
    return witnessRequestRepository
        .findByContractIdAndWitnessId(docId, witnessId)
        .switchIfEmpty(Mono.error(new NotFoundException("Witness not found for this document")))
        .flatMap(
            witness ->
                verifyMyId(witnessId, code, platform)
                    .then(applyWitnessAccept(witness, docId, witnessId)));
  }

  /** Tasdiqsiz ("oddiy") guvoh qabul: template verification turi NONE/null bo'lganda
   *  OTP/MyID talab qilinmaydi — to'g'ridan-to'g'ri ACCEPTED. */
  @Transactional
  public Mono<Void> acceptWitnessSimple(final UUID docId, final UUID witnessId) {
    return witnessRequestRepository
        .findByContractIdAndWitnessId(docId, witnessId)
        .switchIfEmpty(Mono.error(new NotFoundException("Witness not found for this document")))
        .flatMap(witness -> applyWitnessAccept(witness, docId, witnessId));
  }

  // Guvohlik so'rovini ACCEPTED qiladi + qabul qilingan guvohni witnesses jadvaliga yozadi + notification.
  private Mono<Void> applyWitnessAccept(
      WitnessRequestEntity request, UUID docId, UUID witnessId) {
    if (request.getStatus() == DocumentWitnessStatus.ACCEPTED) {
      return Mono.empty();
    }
    request.setStatus(DocumentWitnessStatus.ACCEPTED);
    request.setLastModifiedDate(Instant.now());
    DocumentWitnessEntity accepted = new DocumentWitnessEntity();
    accepted.setContractId(docId);
    accepted.setWitnessId(witnessId);
    return witnessRequestRepository
        .save(request)
        // qabul qilingach — witnesses jadvaliga (status yo'q, mavjudlik = qabul qilingan)
        .then(documentWitnessRepository.save(accepted))
        // guvoh qabul qildi -> creator'ga notification
        .delayUntil(saved -> sendWitnessNotification(docId, witnessId, true))
        // Sessiya/qurilma metasi zanjir ichida olinadi (security context reaktiv).
        .flatMap(
            saved ->
                ContractTimelinePublisher.currentMeta()
                    .doOnNext(
                        meta ->
                            timelinePublisher.publish(
                                docId, "WITNESS_SIGNED", "WITNESS", null, meta))
                    .thenReturn(saved))
        .then();
  }

  // ---------- Taraf (party) qabul: SMS yoki MyID ----------

  /** SMS OTP bilan taraf imzolaydi. userPackageId — yaratuvchi tanlagan paket (ixtiyoriy). */
  @Transactional
  public Mono<Void> acceptParty(UUID docId, UUID userId, Integer otpCode, UUID userPackageId) {
    return documentQueryHelper
        .findAndValidateParty(docId, userId)
        .flatMap(
            doc ->
                verificationService
                    .verifyOtp(userId, docId, ActionType.PARTY_ACCEPT, otpCode)
                    .then(applyPartyAccept(doc, userId, userPackageId)));
  }

  /** Tasdiqsiz ("oddiy") taraf qabul: FAQAT template verification turi NONE/null
   *  bo'lganda ruxsat — server tomonda tekshiriladi (OTP bypass bo'lmasligi uchun). */
  @Transactional
  public Mono<Void> acceptPartySimple(UUID docId, UUID userId, UUID userPackageId) {
    return documentQueryHelper
        .findAndValidateParty(docId, userId)
        .flatMap(
            doc ->
                templateRepository
                    .findById(doc.getTemplateId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Template not found")))
                    .flatMap(
                        template -> {
                          VerificationType type = template.getIndividualVerificationType();
                          if (type != null && type != VerificationType.NONE) {
                            return Mono.error(
                                new BadRequestException(
                                    "Template requires verification: " + type));
                          }
                          return applyPartyAccept(doc, userId, userPackageId);
                        }));
  }

  /** MyID (yuz) bilan taraf imzolaydi. userPackageId — yaratuvchi tanlagan paket (ixtiyoriy). */
  @Transactional
  public Mono<Void> signPartyByMyId(
      UUID docId, UUID userId, String code, String platform, UUID userPackageId) {
    return documentQueryHelper
        .findAndValidateParty(docId, userId)
        .flatMap(
            doc -> verifyMyId(userId, code, platform).then(applyPartyAccept(doc, userId, userPackageId)));
  }

  /** AbleID (yuz, liveness) bilan taraf imzolaydi — attempt webhook orqali SUCCESS bo'lgan. */
  @Transactional
  public Mono<Void> signPartyByAbleId(UUID docId, UUID userId, String attemptId, UUID userPackageId) {
    return documentQueryHelper
        .findAndValidateParty(docId, userId)
        .flatMap(
            doc -> verifyAbleId(userId, attemptId).then(applyPartyAccept(doc, userId, userPackageId)));
  }

  /** AbleID bilan guvoh qabul qiladi. */
  @Transactional
  public Mono<Void> signWitnessByAbleId(final UUID docId, final UUID witnessId, final String attemptId) {
    return witnessRequestRepository
        .findByContractIdAndWitnessId(docId, witnessId)
        .switchIfEmpty(Mono.error(new NotFoundException("Witness not found for this document")))
        .flatMap(
            witness ->
                verifyAbleId(witnessId, attemptId)
                    .then(applyWitnessAccept(witness, docId, witnessId)));
  }

  // AbleID attempt'ni tekshiradi: webhook kelib SUCCESS bo'lgan va sessiya
  // imzolovchining o'z PINFL'iga ochilgan bo'lishi shart.
  private Mono<Void> verifyAbleId(UUID userId, String attemptId) {
    if (attemptId == null || attemptId.isBlank()) {
      return Mono.error(new BadRequestException("AbleID attemptId talab qilinadi"));
    }
    return integrationServiceClient
        .verifyAbleId(attemptId)
        .flatMap(
            res -> {
              if (!"SUCCESS".equalsIgnoreCase(res.status())) {
                return Mono.error(
                    new BadRequestException("AbleID tasdiqlash yakunlanmagan, qayta urinib ko'ring"));
              }
              if (res.pinfl() == null || res.pinfl().isBlank()) {
                return Mono.error(new BadRequestException("AbleID'dan shaxs ma'lumoti olinmadi"));
              }
              return userServiceClient
                  .getUserById(userId)
                  .flatMap(
                      user -> {
                        String userPinfl = user.in();
                        if (userPinfl != null && userPinfl.equals(res.pinfl())) {
                          return Mono.<Void>empty();
                        }
                        return Mono.error(
                            new ForbiddenException(
                                "AbleID orqali tasdiqlangan shaxs hisobingizga mos kelmadi"));
                      });
            })
        .then();
  }

  // Shartnomani bekor qilish endi cancel_requests orqali (CancelRequestsService).

  // taraf statusini ACCEPTED qilib, document statusini yangilaydi + signature
  // yozuvi (Log tab uchun) + notification
  private Mono<Void> applyPartyAccept(DocumentEntity doc, UUID userId) {
    return applyPartyAccept(doc, userId, null);
  }

  private Mono<Void> applyPartyAccept(DocumentEntity doc, UUID userId, UUID userPackageId) {
    return ensureWitnessesSigned(doc.getId())
        .then(documentQueryHelper.isBuyer(doc, userId))
        .flatMap(
            isBuyer ->
                doApplyPartyAccept(doc, userId, isBuyer)
                    // Billing: paket YARATUVCHI o'z imzosini qo'yganda yechiladi (yaratishda
                    // emas). Best-effort — paket xatosi imzolash oqimini buzmaydi.
                    .then(recordCreatorPackageUsage(doc, userId, isBuyer, userPackageId)));
  }

  // Imzolovchi shartnoma YARATUVCHISI bo'lsa, paket foydalanishini qayd etadi.
  // creator_in null (eski hujjatlar — paketi yaratishda yechilgan) -> hech narsa qilmaydi.
  private Mono<Void> recordCreatorPackageUsage(
      DocumentEntity doc, UUID userId, boolean isBuyer, UUID userPackageId) {
    String creatorIn = doc.getCreatorIn();
    if (creatorIn == null || creatorIn.isBlank()) {
      return Mono.empty();
    }
    boolean signerIsCreator =
        isBuyer ? creatorIn.equals(doc.getBuyerIn()) : creatorIn.equals(doc.getSellerIn());
    if (!signerIsCreator) {
      return Mono.empty();
    }
    return userServiceClient
        .recordPackageUsage(userId, userPackageId, doc.getTemplateId(), doc.getId())
        .onErrorResume(e -> Mono.empty());
  }

  private Mono<Void> doApplyPartyAccept(DocumentEntity doc, UUID userId, boolean isBuyer) {
    if (isBuyer) {
      doc.setBuyerStatus(DocumentPartyStatus.ACCEPTED);
    } else {
      doc.setSellerStatus(DocumentPartyStatus.ACCEPTED);
    }

    // Faqat ikkala tomon imzolaganda -> ACTIVE. Bitta tomon imzolasa status
    // CREATED (pending) holatda qoladi; kim imzolagani buyerStatus/sellerStatus'dan.
    boolean bothAccepted =
        DocumentPartyStatus.ACCEPTED == doc.getBuyerStatus()
            && DocumentPartyStatus.ACCEPTED == doc.getSellerStatus();
    if (bothAccepted) {
      doc.setStatus(DocumentStatus.ACTIVE);
    }

    SignatureActionType actionType =
        isBuyer ? SignatureActionType.SIGN_BY_BUYER : SignatureActionType.SIGN_BY_SELLER;

    return documentRepository
        .save(doc)
        // SMS/MyID imzo yozuvi — Log tab'da ko'rinishi uchun
        .flatMap(saved -> saveSignatureRecord(doc.getId(), userId, actionType).thenReturn(saved))
        // imzolashni notification sifatida yuborish
        .delayUntil(saved -> sendPartyAcceptNotification(doc, userId, bothAccepted))
        .flatMap(
            saved ->
                ContractTimelinePublisher.currentMeta()
                    .doOnNext(
                        meta ->
                            timelinePublisher.publish(
                                doc.getId(),
                                "SIGNED",
                                isBuyer ? "BUYER" : "SELLER",
                                isBuyer ? doc.getBuyerIn() : doc.getSellerIn(),
                                meta))
                    .thenReturn(saved))
        .doOnSuccess(
            saved -> {
              String actorIn = isBuyer ? doc.getBuyerIn() : doc.getSellerIn();
              // OpenAPI webhook: har imzoga CONTRACT_SIGNED, ikkalasi imzolasa ACTIVE.
              webhookEventPublisher.publish(WebhookEventType.CONTRACT_SIGNED, doc, actorIn);
              if (bothAccepted) {
                webhookEventPublisher.publish(WebhookEventType.CONTRACT_ACTIVE, doc, actorIn);
              }
            })
        // ACTIVE bo'lganda to'lov jadvallaridagi snapshot'ni yangilaymiz.
        .then(bothAccepted ? syncPaymentContractStatus(doc) : Mono.<Long>empty())
        .then();
  }

  // SMS/MyID taraf imzosini DocumentSignature sifatida yozadi (pkcs7 yo'q —
  // signature maydoniga action turi yoziladi). E-imzo o'z yozuvini alohida yozadi.
  private Mono<DocumentSignatureEntity> saveSignatureRecord(
      UUID docId, UUID userId, SignatureActionType actionType) {
    DocumentSignatureEntity entity = new DocumentSignatureEntity();
    entity.setDocumentId(docId);
    entity.setUserId(userId);
    entity.setActionType(actionType);
    entity.setSignature(actionType.name());
    entity.setCreatedDate(Instant.now());
    return documentSignatureRepository.save(entity);
  }

  // MyID code'ni tekshiradi: pinfl imzolovchining o'ziga tegishli.
  // platform (WEB/MOBILE) qaysi SDK credential bilan tekshirishni belgilaydi.
  // SDK oqimida comparisonValue qaytadi — threshold tekshiriladi.
  private Mono<Void> verifyMyId(UUID userId, String code, String platform) {
    return integrationServiceClient
        .verifyMyId(code, userId, platform)
        .flatMap(
            res -> {
              if (res.comparisonValue() != null && res.comparisonValue() < MYID_MIN_COMPARISON) {
                return Mono.error(
                    new BadRequestException("MyID yuz mosligi yetarli emas, qayta urinib ko'ring"));
              }
              if (res.pinfl() == null || res.pinfl().isBlank()) {
                return Mono.error(new BadRequestException("MyID'dan shaxs ma'lumoti olinmadi"));
              }
              // Imzolovchining hisobidagi pinfl (in) MyID tasdiqlagan pinfl'ga mos
              // kelishi shart. getUserById — autentifikatsiya qilingan userning o'zi
              // (getUserByIn 404 → 500 muammosidan qochamiz).
              return userServiceClient
                  .getUserById(userId)
                  .flatMap(
                      user -> {
                        String userPinfl = user.in();
                        if (userPinfl != null && userPinfl.equals(res.pinfl())) {
                          return Mono.<Void>empty();
                        }
                        return Mono.error(
                            new ForbiddenException(
                                "MyID orqali tasdiqlangan shaxs hisobingizga mos kelmadi"));
                      });
            })
        .then();
  }

  @Transactional
  public Mono<Void> rejectWitness(UUID docId, UUID witnessId) {
    return witnessRequestRepository
        .findAllByContractId(docId)
        .filter(w -> w.getWitnessId().equals(witnessId))
        .next()
        .switchIfEmpty(Mono.error(new NotFoundException("Witness not found")))
        .flatMap(
            witness -> {
              witness.setStatus(DocumentWitnessStatus.REJECTED);
              witness.setLastModifiedDate(Instant.now());
              return witnessRequestRepository
                  .save(witness)
                  // guvoh rad etdi -> creator'ga notification
                  .delayUntil(saved -> sendWitnessNotification(docId, witnessId, false))
                  .flatMap(
                      saved ->
                          ContractTimelinePublisher.currentMeta()
                              .doOnNext(
                                  meta ->
                                      timelinePublisher.publish(
                                          docId, "WITNESS_REJECTED", "WITNESS", null, meta))
                              .thenReturn(saved))
                  .then();
            });
  }

  @Transactional
  public Mono<Void> rejectParty(UUID docId, UUID userId) {
    return documentQueryHelper
        .findAndValidateParty(docId, userId)
        .flatMap(
            doc ->
                // Joriy user PINFL'ini bir marta olamiz — taraf (buyer?) va yaratuvchi tekshiruvi.
                userServiceClient
                    .getUserById(userId)
                    .flatMap(
                        me -> {
              String myIn = me.in();
              boolean isBuyer = myIn != null && myIn.equals(doc.getBuyerIn());
              // yaratuvchi bekor qilsa -> CANCELLED, boshqa tomon -> REJECTED
              boolean isCanceller = myIn != null && myIn.equals(doc.getCreatorIn());
              if (isCanceller) {
                doc.setStatus(DocumentStatus.CANCELLED);
              } else {
                doc.setStatus(DocumentStatus.REJECTED);
              }

              if (isBuyer) {
                doc.setBuyerStatus(DocumentPartyStatus.REJECTED);
              } else {
                doc.setSellerStatus(DocumentPartyStatus.REJECTED);
              }

              return documentRepository
                  .save(doc)
                  // rad/bekor qilish notification'ni yuborish
                  .delayUntil(saved -> sendPartyRejectNotification(doc, userId, isCanceller))
                  // OpenAPI webhook: yaratuvchi bekor qilsa CANCELLED, boshqa taraf REJECTED.
                  .doOnSuccess(
                      saved ->
                          webhookEventPublisher.publish(
                              isCanceller
                                  ? WebhookEventType.CONTRACT_CANCELLED
                                  : WebhookEventType.CONTRACT_REJECTED,
                              doc,
                              myIn))
                  // CANCELLED/REJECTED bo'ldi — to'lov jadvallaridagi snapshot'ni yangilaymiz.
                  .then(syncPaymentContractStatus(doc))
                  .then();
                        }));
  }

  // Shartnoma holati o'zgargach — o'sha shartnomaning barcha to'lov jadvallaridagi
  // snapshot (contract_status) ni yangilaydi (B2BPaymentScheduleResponse uchun).
  private Mono<Long> syncPaymentContractStatus(DocumentEntity doc) {
    return paymentScheduleRepository.updateContractStatusByContractId(
        doc.getId(), doc.getStatus().name());
  }

  // ======================== NOTIFICATION HELPERS ========================

  // guvoh accept/reject -> document creator'ga notification
  private Mono<Void> sendWitnessNotification(UUID docId, UUID witnessId, boolean accepted) {
    return Mono.zip(
            documentQueryHelper.findOrThrow(docId), userServiceClient.getUserById(witnessId))
        .flatMap(
            tuple -> {
              DocumentEntity doc = tuple.getT1();
              UserResponse witness = tuple.getT2();
              String docNumber = doc.getNumber();

              // creator endi PINFL — notification recipient UUID'sini tiklaymiz (topilmasa skip).
              return documentQueryHelper
                  .resolveUserIdByIn(doc.getCreatorIn())
                  .flatMap(
                      creatorId -> {
                        FirebaseNotificationReply notification =
                            accepted
                                ? FirebaseNotificationReply.witnessAccepted(
                                    docId, creatorId, docNumber, witness.fullName())
                                : FirebaseNotificationReply.witnessRejected(
                                    docId, creatorId, docNumber, witness.fullName());
                        return publishNotification(notification);
                      });
            })
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to send witness notification for doc [{}]: {}", docId, e.getMessage());
              return Mono.empty();
            });
  }

  // party accept -> boshqa tomonlarga + guvohlar ga notification
  private Mono<Void> sendPartyAcceptNotification(
      DocumentEntity doc, UUID signerId, boolean bothAccepted) {
    UUID docId = doc.getId();
    String docNumber = doc.getNumber();

    return Mono.zip(
            userServiceClient.getUserById(signerId),
            userServiceClient.getUsersByIns(java.util.List.of(doc.getBuyerIn(), doc.getSellerIn())))
        .flatMap(
            tup -> {
              UserResponse signer = tup.getT1();
              java.util.Map<String, UserResponse> parties = tup.getT2();
              UserResponse buyer = parties.get(doc.getBuyerIn());
              UserResponse seller = parties.get(doc.getSellerIn());
              UUID buyerId = buyer != null ? buyer.id() : null;
              UUID sellerId = seller != null ? seller.id() : null;
              boolean signerIsBuyer = signer.in() != null && signer.in().equals(doc.getBuyerIn());
              // boshqa tomonga "imzoladi" notification
              UUID otherPartyId = signerIsBuyer ? sellerId : buyerId;

              Mono<Void> signedNotif =
                  publishNotification(
                      FirebaseNotificationReply.documentSigned(
                          docId, otherPartyId, docNumber, signer.fullName()));

              if (bothAccepted) {
                // ikkala tomon + barcha guvohlar ga "completed" notification
                Mono<Void> completedToBuyer =
                    publishNotification(
                        FirebaseNotificationReply.documentCompleted(docId, buyerId, docNumber));
                Mono<Void> completedToSeller =
                    publishNotification(
                        FirebaseNotificationReply.documentCompleted(docId, sellerId, docNumber));
                Mono<Void> completedToWitnesses =
                    documentWitnessRepository
                        .findAllByContractId(docId)
                        .flatMap(
                            w ->
                                publishNotification(
                                    FirebaseNotificationReply.documentCompleted(
                                        docId, w.getWitnessId(), docNumber)))
                        .then();

                return signedNotif
                    .then(completedToBuyer)
                    .then(completedToSeller)
                    .then(completedToWitnesses);
              }
              return signedNotif;
            })
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to send accept notification for doc [{}]: {}", docId, e.getMessage());
              return Mono.empty();
            });
  }

  // party reject/cancel -> boshqa tomonlarga + guvohlar ga notification
  private Mono<Void> sendPartyRejectNotification(
      DocumentEntity doc, UUID rejecterId, boolean isCanceller) {
    UUID docId = doc.getId();
    String docNumber = doc.getNumber();

    return Mono.zip(
            userServiceClient.getUserById(rejecterId),
            userServiceClient.getUsersByIns(java.util.List.of(doc.getBuyerIn(), doc.getSellerIn())))
        .flatMap(
            tup -> {
              UserResponse rejecter = tup.getT1();
              java.util.Map<String, UserResponse> parties = tup.getT2();
              UserResponse buyer = parties.get(doc.getBuyerIn());
              UserResponse seller = parties.get(doc.getSellerIn());
              boolean rejecterIsBuyer =
                  rejecter.in() != null && rejecter.in().equals(doc.getBuyerIn());
              UUID otherPartyId =
                  rejecterIsBuyer
                      ? (seller != null ? seller.id() : null)
                      : (buyer != null ? buyer.id() : null);

              // boshqa tomonga notification
              FirebaseNotificationReply partyNotif =
                  isCanceller
                      ? FirebaseNotificationReply.documentCancelled(
                          docId, otherPartyId, docNumber, rejecter.fullName())
                      : FirebaseNotificationReply.documentRejected(
                          docId, otherPartyId, docNumber, rejecter.fullName());

              Mono<Void> notifyParty = publishNotification(partyNotif);

              // guvohlar ga ham notification
              FirebaseNotificationReply witnessNotifTemplate =
                  isCanceller
                      ? FirebaseNotificationReply.documentCancelled(
                          docId, null, docNumber, rejecter.fullName())
                      : FirebaseNotificationReply.documentRejected(
                          docId, null, docNumber, rejecter.fullName());

              Mono<Void> notifyWitnesses =
                  documentWitnessRepository
                      .findAllByContractId(docId)
                      .flatMap(
                          w -> {
                            FirebaseNotificationReply wNotif =
                                isCanceller
                                    ? FirebaseNotificationReply.documentCancelled(
                                        docId, w.getWitnessId(), docNumber, rejecter.fullName())
                                    : FirebaseNotificationReply.documentRejected(
                                        docId, w.getWitnessId(), docNumber, rejecter.fullName());
                            return publishNotification(wNotif);
                          })
                      .then();

              return notifyParty.then(notifyWitnesses);
            })
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to send reject notification for doc [{}]: {}", docId, e.getMessage());
              return Mono.empty();
            });
  }

  // RabbitMQ orqali notification service'ga yuborish
  private Mono<Void> publishNotification(FirebaseNotificationReply notification) {
    if (notification == null || notification.toUserId() == null) return Mono.empty();
    return jmsPublisher
        .publish(notification)
        .onErrorResume(
            e -> {
              log.error("Failed to publish notification: {}", e.getMessage());
              return Mono.empty();
            });
  }

  private Mono<UUID> findDocumentOrWitness(UUID documentId, ActionType action, UUID userId) {
    if (action == ActionType.WITNESS_ACCEPT) {
      // Guvohlik OTP yuborishda — guvohlik so'rovi (witness_requests) bo'yicha tekshiramiz.
      return witnessRequestRepository
          .findAllByContractId(documentId)
          .filter(w -> w.getWitnessId().equals(userId))
          .next()
          .switchIfEmpty(
              Mono.error(new ForbiddenException("User is not a witness for this document")))
          .map(WitnessRequestEntity::getWitnessId);
    } else {
      return documentQueryHelper.findAndValidateParty(documentId, userId).map(doc -> userId);
    }
  }
}
