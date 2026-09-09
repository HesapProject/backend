package uz.hesap.service.document.service.c2c;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;
import uz.hesap.service.document.model.response.WitnessResponse;
import org.springframework.data.domain.PageRequest;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.repository.CustomDocumentRepository;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.repository.WitnessRequestRepository;
import uz.hesap.service.document.service.document.ContractsService;
import uz.hesap.service.document.service.template.TemplateNotificationDispatcher;
import uz.hesap.service.document.webclient.UserServiceClient;

// Guvohlik so'rovlari (witness_requests) — guvohlikka chaqirilganlar ro'yxati (status bilan).
@Service
@RequiredArgsConstructor
public class WitnessRequestsService {

  private final WitnessRequestRepository witnessRequestRepository;
  private final UserServiceClient userServiceClient;
  private final DocumentRepository documentRepository;
  private final CustomDocumentRepository customDocumentRepository;
  private final ContractsService contractsService;
  private final TemplateNotificationDispatcher notificationDispatcher;

  // Shartnoma hali imzolanmagan (guvoh qo'shish/olib tashlash mumkin bo'lgan) holatlar.
  private static final Set<DocumentStatus> EDITABLE_STATUSES =
      Set.of(
          DocumentStatus.CREATED,
          DocumentStatus.SIGNED_BY_BUYER,
          DocumentStatus.SIGNED_BY_SELLER);

  // Shartnoma bo'yicha guvohlik so'rovlari — guvoh (witnessId → user) bilan boyitiladi.
  public Flux<WitnessResponse> getByContract(UUID contractId) {
    return witnessRequestRepository
        .findAllByContractId(contractId)
        .collectList()
        .flatMapMany(
            requests -> {
              if (requests.isEmpty()) {
                return Flux.empty();
              }
              List<UUID> ids =
                  requests.stream().map(WitnessRequestEntity::getWitnessId).toList();
              return userServiceClient
                  .getUsersByIds(ids)
                  .collectMap(UserResponse::id, Function.identity())
                  .flatMapMany(
                      users ->
                          Flux.fromIterable(requests)
                              .map(
                                  r ->
                                      new WitnessResponse(
                                          users.get(r.getWitnessId()),
                                          r.getStatus(),
                                          r.getLastModifiedDate())));
            });
  }

  // Menga kelgan guvohlik takliflari (men guvohman, PENDING) — shartnomalar enrichment bilan.
  // Home "Guvohlik" incoming karta uchun.
  public Mono<List<DocumentEnrichedResponse>> getIncoming(UUID witnessUserId, String myIn) {
    return witnessRequestRepository
        .findAllByWitnessIdAndStatus(witnessUserId, DocumentWitnessStatus.PENDING)
        .map(WitnessRequestEntity::getContractId)
        .distinct()
        .collectList()
        .flatMap(contractsService::enrichByIds)
        .map(
            list ->
                list.stream()
                    // Taraf o'zi guvoh sifatida ko'rinmasin (eski/xato yozuvlar himoyasi):
                    // buyer_in/seller_in = mening PINFL bo'lsa — chiqarib tashlaymiz.
                    .filter(d -> !partySelf(d, myIn))
                    // Yakunlangan/rad/bekor shartnomada guvohlik taklifi ahamiyatsiz.
                    .filter(d -> isWitnessRelevant(d.status()))
                    .toList());
  }

  // Men guvohlik BERGAN (ACCEPTED) shartnomalar — More > Guvohliklar ro'yxati uchun.
  // Shartnoma statusi filtrlanmaydi: klient faol/arxiv bo'lib o'zi ajratadi.
  public Mono<List<DocumentEnrichedResponse>> getMine(UUID witnessUserId, String myIn) {
    return witnessRequestRepository
        .findAllByWitnessIdAndStatus(witnessUserId, DocumentWitnessStatus.ACCEPTED)
        .map(WitnessRequestEntity::getContractId)
        .distinct()
        .collectList()
        .flatMap(contractsService::enrichByIds)
        .map(list -> list.stream().filter(d -> !partySelf(d, myIn)).toList());
  }

  private static boolean partySelf(DocumentEnrichedResponse d, String myIn) {
    return myIn != null
        && (myIn.equals(d.buyerIn()) || myIn.equals(d.sellerIn()));
  }

  private static boolean isWitnessRelevant(DocumentStatus status) {
    return status == DocumentStatus.CREATED
        || status == DocumentStatus.SIGNED_BY_BUYER
        || status == DocumentStatus.SIGNED_BY_SELLER
        || status == DocumentStatus.ACTIVE;
  }

  // Men yuborgan guvohlik so'rovlari (men taraf, guvoh imzosi PENDING kutilyapti) —
  // shartnomalar enrichment bilan. Home "Guvohlik" outgoing karta uchun.
  public Mono<List<DocumentEnrichedResponse>> getOutgoing(String myIn) {
    return customDocumentRepository
        .findFiltered(myIn, null, null, null, null, null, PageRequest.of(0, 500))
        .map(DocumentEntity::getId)
        .collectList()
        .flatMap(
            myIds -> {
              if (myIds.isEmpty()) {
                return Mono.just(List.<DocumentEnrichedResponse>of());
              }
              return witnessRequestRepository
                  .findAllByContractIdInAndStatus(myIds, DocumentWitnessStatus.PENDING)
                  .map(WitnessRequestEntity::getContractId)
                  .distinct()
                  .collectList()
                  .flatMap(contractsService::enrichByIds)
                  .map(
                      list ->
                          list.stream()
                              // Rad/bekor/yakunlangan shartnomada guvohlik ahamiyatsiz.
                              .filter(d -> isWitnessRelevant(d.status()))
                              .toList());
            });
  }

  // Mavjud (imzolanmagan) shartnomaga guvoh qo'shadi. Faqat taraf (buyer/seller), shartnoma
  // hali imzolanmagan bo'lsa; takror qo'shilmaydi; guvohga taklif notification yuboriladi.
  public Mono<Void> addWitness(UUID contractId, UUID witnessId, String callerIdentifier) {
    return loadEditableContract(contractId, callerIdentifier)
        .flatMap(
            doc ->
                witnessRequestRepository
                    .findByContractIdAndWitnessId(contractId, witnessId)
                    .flatMap(existing -> Mono.<Void>error(new BadRequestException("Guvoh allaqachon qo'shilgan")))
                    .switchIfEmpty(
                        Mono.defer(
                            () ->
                                ensureWitnessNotParty(doc, witnessId)
                                    .then(Mono.defer(() -> createWitness(doc, witnessId))))));
  }

  // Guvohni olib tashlaydi — faqat taraf, shartnoma imzolanmagan va guvoh hali imzolamagan
  // (status PENDING) bo'lsa.
  public Mono<Void> removeWitness(UUID contractId, UUID witnessId, String callerIdentifier) {
    return loadEditableContract(contractId, callerIdentifier)
        .flatMap(
            doc ->
                witnessRequestRepository
                    .findByContractIdAndWitnessId(contractId, witnessId)
                    .switchIfEmpty(Mono.error(new NotFoundException("Guvohlik so'rovi topilmadi")))
                    .flatMap(
                        req -> {
                          if (req.getStatus() != DocumentWitnessStatus.PENDING) {
                            return Mono.error(
                                new BadRequestException("Imzolagan guvohni olib tashlab bo'lmaydi"));
                          }
                          return witnessRequestRepository.delete(req);
                        }));
  }

  // Shartnomani yuklab, imzolanmaganligini va chaqiruvchi taraf ekanini tekshiradi.
  private Mono<DocumentEntity> loadEditableContract(UUID contractId, String callerIdentifier) {
    return documentRepository
        .findByIdAndDeletedFalse(contractId)
        .switchIfEmpty(Mono.error(new NotFoundException("Shartnoma topilmadi")))
        .flatMap(
            doc -> {
              if (!EDITABLE_STATUSES.contains(doc.getStatus())) {
                return Mono.error(
                    new BadRequestException("Shartnoma imzolangan — guvohlarni o'zgartirib bo'lmaydi"));
              }
              if (!isParty(doc, callerIdentifier)) {
                return Mono.error(new ForbiddenException("Faqat shartnoma tarafi guvoh boshqaradi"));
              }
              return Mono.just(doc);
            });
  }

  // Guvoh shartnoma tarafi (buyer/seller) bo'lmasligini tekshiradi — taraf guvoh bo'la olmaydi.
  // Guvoh egasining barqaror identifikatori (PINFL/STIR) doc.buyerIn/sellerIn bilan solishtiriladi.
  private Mono<Void> ensureWitnessNotParty(DocumentEntity doc, UUID witnessId) {
    return userServiceClient
        .getUserById(witnessId)
        .flatMap(
            user -> {
              String witnessIn = user.identifier();
              if (witnessIn != null && isParty(doc, witnessIn)) {
                return Mono.<Void>error(
                    new BadRequestException("Shartnoma tarafi guvoh bo'la olmaydi"));
              }
              return Mono.<Void>empty();
            });
  }

  // Guvoh yozuvini (PENDING) saqlab, taklif notification yuboradi.
  private Mono<Void> createWitness(DocumentEntity doc, UUID witnessId) {
    WitnessRequestEntity entity = new WitnessRequestEntity();
    entity.setContractId(doc.getId());
    entity.setWitnessId(witnessId);
    entity.setStatus(DocumentWitnessStatus.PENDING);
    return witnessRequestRepository
        .save(entity)
        .flatMap(
            saved ->
                notificationDispatcher.dispatch(
                    doc.getTemplateId(),
                    NotificationEvent.WITNESS,
                    witnessId,
                    doc.getNumber(),
                    null,
                    doc.getId()))
        .then();
  }

  // Chaqiruvchi (identifier = PINFL/STIR) shartnoma tarafimi (buyer/seller).
  private boolean isParty(DocumentEntity doc, String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return false;
    }
    return identifier.equals(doc.getBuyerIn()) || identifier.equals(doc.getSellerIn());
  }
}
