package uz.hesap.service.document.service.payment;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.*;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;
import uz.hesap.service.document.model.mapper.PaymentScheduleMapper;
import uz.hesap.service.document.model.response.*;
import uz.hesap.service.document.repository.*;
import uz.hesap.service.document.service.DocumentMetadataService;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentScheduleQueryService {

  private final PaymentScheduleRepository paymentScheduleRepository;
  private final PaymentScheduleRequestRepository paymentScheduleRequestRepository;
  private final PaidScheduleRepository paidScheduleRepository;
  private final CustomPaymentScheduleRequestRepositoryImpl
      customPaymentScheduleRequestRepositoryImpl;
  private final CustomPaymentScheduleRepositoryImpl customPaymentScheduleRepositoryImpl;
  private final DocumentMetadataService metadataService;
  private final DocumentRepository documentRepository;

  // ================ PAYMENT SCHEDULE ================

  // hujjatdagi barcha to'lov jadvallari
  public Flux<PaymentScheduleResponse> getAllByDocumentId(UUID documentId, boolean withFullInfo) {
    return paymentScheduleRepository
        .findAllByContractIdAndDeletedFalse(documentId)
        .collectList()
        .flatMapMany(list -> mapPaymentSchedules(list, withFullInfo));
  }

  // bitta to'lov jadvali
  public Mono<PaymentScheduleResponse> getById(UUID id, boolean withFullInfo) {
    return paymentScheduleRepository
        .findById(id)
        .flatMap(e -> mapPaymentSchedules(List.of(e), withFullInfo).next());
  }

  // ================ PAYMENT REQUEST ================

  // hujjatdagi barcha to'lov so'rovlari
  public Flux<PaymentScheduleRequestResponse> getAllRequestsByDocumentId(
      UUID documentId, boolean withFullInfo) {
    return paymentScheduleRequestRepository
        .findAllByContractIdAndDeletedFalse(documentId)
        .collectList()
        .flatMapMany(list -> mapPaymentRequests(list, withFullInfo));
  }

  // bitta to'lov so'rovi
  public Mono<PaymentScheduleRequestResponse> getRequestById(UUID id, boolean withFullInfo) {
    return paymentScheduleRequestRepository
        .findById(id)
        .flatMap(e -> mapPaymentRequests(List.of(e), withFullInfo).next());
  }

  // Haqdorga (seller) kelgan PENDING to'lov so'rovlari — shartnoma raqami va
  // yuborgan user bilan (So'rovlarim sahifasi).
  public Flux<IncomingPaymentRequestResponse> getIncomingRequests(UUID userId) {
    return customPaymentScheduleRequestRepositoryImpl
        .findIncomingPending(userId)
        .collectList()
        .flatMapMany(
            list -> {
              if (list.isEmpty()) {
                return Flux.empty();
              }
              Set<UUID> docIds = new HashSet<>();
              list.forEach(e -> docIds.add(e.getContractId()));
              // hujjat raqamlari + user enrich parallel
              Mono<Map<UUID, String>> numbersMono =
                  documentRepository
                      .findAllById(docIds)
                      .collectMap(d -> d.getId(), d -> d.getNumber());
              Mono<List<PaymentScheduleRequestResponse>> requestsMono =
                  mapPaymentRequests(list, true).collectList();
              return Mono.zip(numbersMono, requestsMono)
                  .flatMapMany(
                      t ->
                          Flux.fromIterable(t.getT2())
                              .map(
                                  r ->
                                      new IncomingPaymentRequestResponse(
                                          r.id(),
                                          r.contractId(),
                                          t.getT1().get(r.contractId()),
                                          null,
                                          r.status(),
                                          r.paymentId(),
                                          r.amount(),
                                          r.paymentDate(),
                                          r.note(),
                                          r.image(),
                                          r.createdDate())));
            });
  }

  // bitta paymentScheduleId bo'yicha barcha requestlar
  public Flux<PaymentScheduleRequestResponse> getRequestsByPaymentScheduleId(
      UUID paymentScheduleId, boolean withFullInfo) {
    return paymentScheduleRequestRepository
        .findAllByPaymentIdAndDeletedFalse(paymentScheduleId)
        .collectList()
        .flatMapMany(list -> mapPaymentRequests(list, withFullInfo));
  }

  // Filtrlangan to'lov so'rovlari: ?paymentId, ?receiverIn, ?fromIn (yuboruvchi=buyer),
  // ?toIn (qabul qiluvchi=seller), ?statuses.
  public Flux<PaymentScheduleRequestResponse> getRequests(
      java.util.UUID paymentId,
      String receiverIn,
      String fromIn,
      String toIn,
      java.util.List<uz.hesap.service.document.domain.enums.PaymentScheduleStatus> statuses) {
    return customPaymentScheduleRequestRepositoryImpl
        .findRequests(paymentId, receiverIn, fromIn, toIn, statuses)
        .collectList()
        .flatMapMany(list -> mapPaymentRequests(list, true));
  }

  // Kechiktirish so'rovlari (type=DELAY): ?buyerIn, ?sellerIn, ?fromIn, ?toIn,
  // ?contractId, ?paymentId, ?statuses.
  public Flux<uz.hesap.service.document.model.response.DelayRequestListResponse> getDelayRequests(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      java.util.UUID contractId,
      java.util.UUID paymentId,
      java.util.List<uz.hesap.service.document.domain.enums.PaymentScheduleStatus> statuses) {
    // Ro'yxat SQL'ning o'zida boyitiladi (shartnoma raqami, so'rovchi ismi, nechanchi to'lov).
    return customPaymentScheduleRequestRepositoryImpl.findDelayRequestsEnriched(
        buyerIn, sellerIn, fromIn, toIn, contractId, paymentId, statuses);
  }

  // ================ PAID SCHEDULE ================

  // hujjatdagi barcha amalga oshgan to'lovlar
  public Flux<PaidScheduleResponse> getAllPaidByDocumentId(UUID documentId, boolean withFullInfo) {
    return paidScheduleRepository
        .findAllByDocumentIdAndDeletedFalse(documentId)
        .collectList()
        .flatMapMany(list -> mapPaidSchedules(list, withFullInfo));
  }

  // bitta to'lov
  public Mono<PaidScheduleResponse> getPaidById(UUID id, boolean withFullInfo) {
    return paidScheduleRepository
        .findById(id)
        .flatMap(e -> mapPaidSchedules(List.of(e), withFullInfo).next());
  }

  // paymentSchedule bo'yicha amalga oshgan to'lovlar
  public Flux<PaidScheduleResponse> getPaidByPaymentScheduleId(
      UUID paymentScheduleId, boolean withFullInfo) {
    return paidScheduleRepository
        .findAllByPaymentScheduleIdAndDeletedFalse(paymentScheduleId)
        .collectList()
        .flatMapMany(list -> mapPaidSchedules(list, withFullInfo));
  }

  // ================ SCORE ================

  public Mono<PaymentScoreResponse> getScore(UUID userId) {
    return customPaymentScheduleRepositoryImpl.getScore(userId);
  }

  // ================ MAPPING — withFullInfo bilan / bo'lmasdan ================

  // payment schedule larni map qilish
  private Flux<PaymentScheduleResponse> mapPaymentSchedules(
      List<PaymentEntity> entities, boolean withFullInfo) {
    if (!withFullInfo) {
      // faqat IDlar — user fetch qilmasdan
      return Flux.fromIterable(entities)
          .map(e -> PaymentScheduleMapper.INSTANCE.toResponsePayment(e, null, null));
    }
    return metadataService
        .fetchMetadata(extractPaymentScheduleIds(entities))
        .flatMapMany(
            data ->
                Flux.fromIterable(entities)
                    .map(
                        e ->
                            PaymentScheduleMapper.INSTANCE.toResponsePayment(e, null, null)));
  }

  // payment request larni map qilish
  private Flux<PaymentScheduleRequestResponse> mapPaymentRequests(
      List<PaymentScheduleRequestEntity> entities, boolean withFullInfo) {
    if (!withFullInfo) {
      return mapRequestsWithoutUser(entities);
    }
    // User enrichment xato bersa (user service uvol) — so'rovlar yo'qolmasin,
    // user'siz qaytaramiz (onErrorResume bilan asosiy flow buzilmaydi).
    return metadataService
        .fetchMetadata(extractRequestIds(entities))
        .flatMapMany(
            data ->
                Flux.fromIterable(entities)
                    .map(
                        e ->
                            PaymentScheduleMapper.INSTANCE.toResponseRequest(e)))
        .onErrorResume(err -> mapRequestsWithoutUser(entities));
  }

  // So'rovlarni user ma'lumotisiz mapping qiladi.
  private Flux<PaymentScheduleRequestResponse> mapRequestsWithoutUser(
      List<PaymentScheduleRequestEntity> entities) {
    return Flux.fromIterable(entities)
        .map(e -> PaymentScheduleMapper.INSTANCE.toResponseRequest(e));
  }

  // paid schedule larni map qilish
  private Flux<PaidScheduleResponse> mapPaidSchedules(
      List<PaidScheduleEntity> entities, boolean withFullInfo) {
    if (!withFullInfo) {
      return Flux.fromIterable(entities)
          .map(e -> PaymentScheduleMapper.INSTANCE.toResponsePayment(e, null, null));
    }
    return metadataService
        .fetchMetadata(extractPaidIds(entities))
        .flatMapMany(
            data ->
                Flux.fromIterable(entities)
                    .map(
                        e ->
                            PaymentScheduleMapper.INSTANCE.toResponsePayment(e, null, null)));
  }

  // ================ ID EXTRACTION ================

  private IdExtractionResult extractPaymentScheduleIds(List<PaymentEntity> list) {
    return IdExtractionResult.fromSchedules(list);
  }

  private IdExtractionResult extractRequestIds(List<PaymentScheduleRequestEntity> list) {
    return IdExtractionResult.fromRequests(list);
  }

  private IdExtractionResult extractPaidIds(List<PaidScheduleEntity> list) {
    return IdExtractionResult.fromPaidSchedules(list);
  }
}
