package uz.hesap.service.document.service.payment;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.PaymentFilterStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.model.mapper.PaymentScheduleMapper;
import uz.hesap.service.document.model.response.B2BPaidScheduleResponse;
import uz.hesap.service.document.model.response.B2BPaymentScheduleResponse;
import uz.hesap.service.document.model.response.B2BPaymentStatsResponse;
import uz.hesap.service.document.model.response.IdExtractionResult;
import uz.hesap.service.document.repository.CustomPaymentScheduleRepository;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.webclient.UserServiceClient;

/** B2B va C2C to'lovlar — payment schedule va paid schedule uchun bitta query service. */
@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentsService {

  private static final PaymentScheduleMapper MAPPER = PaymentScheduleMapper.INSTANCE;
  private final CustomPaymentScheduleRepository customPaymentScheduleRepository;
  private final DocumentRepository documentRepository;
  private final UserServiceClient userServiceClient;

  // ================ PAYMENT SCHEDULE (unified) ================

  /**
   * userId bo'yicha to'lov jadvallari filter.
   *
   * @param id userId
   * @param direction INCOME/OUTCOME/null
   * @param statuses status filter
   * @param startDate optional
   * @param endDate optional
   */
  public Mono<Page<B2BPaymentScheduleResponse>> getSchedules(
      UUID id,
      Direction direction,
      List<PaymentScheduleStatus> statuses,
      Instant startDate,
      Instant endDate,
      Pageable pageable) {
    log.debug("Find all payment schedule {} {}", id, direction);

    return customPaymentScheduleRepository
        .findFiltered(id, direction, statuses, startDate, endDate, pageable)
        .collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(new PageImpl<>(List.of(), pageable, 0));
              }

              return enrichAndPage(
                  entities,
                  customPaymentScheduleRepository.countFiltered(
                      id, direction, statuses, startDate, endDate),
                  pageable,
                  this::toScheduleResponse);
            });
  }

  // /payments — sana + buyerIn/sellerIn (hujjat) + contractId + hisoblangan statuses +
  // ixtiyoriy shartnoma (hujjat) holati (contractStatuses) bo'yicha.
  public Mono<Page<B2BPaymentScheduleResponse>> getPaymentsFiltered(
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses,
      Pageable pageable) {
    return customPaymentScheduleRepository
        .findByAdminFilter(
            startDate, endDate, buyerIn, sellerIn, contractId, statuses, contractStatuses, pageable)
        .collectList()
        .flatMap(
            entities ->
                entities.isEmpty()
                    ? Mono.just(new PageImpl<>(List.of(), pageable, 0))
                    : enrichAndPage(
                        entities,
                        customPaymentScheduleRepository.countByAdminFilter(
                            startDate, endDate, buyerIn, sellerIn, contractId, statuses,
                            contractStatuses),
                        pageable,
                        this::toScheduleResponse));
  }

  // ================ PAID SCHEDULE ================

  public Mono<Page<B2BPaidScheduleResponse>> getPaid(
      UUID userId, Direction direction, Pageable pageable) {

    return customPaymentScheduleRepository
        .findPaidByUserFiltered(userId, direction, pageable)
        .collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(new PageImpl<>(List.of(), pageable, 0));
              }

              return enrichAndPage(
                  entities,
                  customPaymentScheduleRepository.countPaidByUserFiltered(userId, direction),
                  pageable,
                  this::toPaidResponse);
            });
  }

  // ================ STATS ================

  public Flux<B2BPaymentStatsResponse> getStats(UUID userId) {
    return customPaymentScheduleRepository.getStats(userId);
  }

  // ================ SHARED: ENRICH + PAGE ================

  private <E, R> Mono<Page<R>> enrichAndPage(
      List<E> entities,
      Mono<Long> countMono,
      Pageable pageable,
      ResponseMapper<E, R> responseMapper) {

    Set<UUID> docIds = new HashSet<>();
    boolean isPayment = !entities.isEmpty() && entities.getFirst() instanceof PaymentEntity;
    boolean isPaid = !entities.isEmpty() && entities.getFirst() instanceof PaidScheduleEntity;

    if (isPayment) {
      entities.forEach(e -> docIds.add(((PaymentEntity) e).getContractId()));
    } else if (isPaid) {
      entities.forEach(e -> docIds.add(((PaidScheduleEntity) e).getDocumentId()));
    }

    // Avval shartnomalarni olamiz; PaymentEntity'da buyer/seller UUID yo'q (PINFL) —
    // user'lar shartnoma (buyerUserId/sellerUserId) orqali hal qilinadi.
    return fetchDocuments(docIds)
        .flatMap(
            docsMap -> {
              // Payment ham, PaidSchedule ham endi PINFL saqlaydi — user'lar shartnoma
              // buyer_in/seller_in (PINFL/STIR) orqali hal qilinadi.
              Set<String> partyIns = new HashSet<>();
              if (isPayment || isPaid) {
                docsMap
                    .values()
                    .forEach(
                        d -> {
                          if (d.getBuyerIn() != null) partyIns.add(d.getBuyerIn());
                          if (d.getSellerIn() != null) partyIns.add(d.getSellerIn());
                        });
              }
              return Mono.zip(fetchUsers(partyIns), countMono)
                  .map(
                      tuple -> {
                        var usersMap = tuple.getT1();
                        long total = tuple.getT2();
                        List<R> responses =
                            entities.stream()
                                .map(e -> responseMapper.map(e, docsMap, usersMap))
                                .toList();
                        return new PageImpl<>(responses, pageable, total);
                      });
            });
  }

  // ================ PRIVATE: RESPONSE MAPPING ================

  private B2BPaymentScheduleResponse toScheduleResponse(
      PaymentEntity e,
      Map<UUID, DocumentEntity> docsMap,
      Map<String, UserBasicResponse> usersMap) {
    DocumentEntity doc = docsMap.get(e.getContractId());
    return MAPPER.toB2BPaymentResponse(
        e,
        doc != null ? doc.getNumber() : null,
        doc != null ? usersMap.get(doc.getBuyerIn()) : null,
        doc != null ? usersMap.get(doc.getSellerIn()) : null);
  }

  private B2BPaidScheduleResponse toPaidResponse(
      PaidScheduleEntity e,
      Map<UUID, DocumentEntity> docsMap,
      Map<String, UserBasicResponse> usersMap) {
    DocumentEntity doc = docsMap.get(e.getDocumentId());
    return MAPPER.toB2BPaidResponse(
        e,
        doc != null ? doc.getNumber() : null,
        doc != null ? usersMap.get(doc.getBuyerIn()) : null,
        doc != null ? usersMap.get(doc.getSellerIn()) : null);
  }

  // ================ PRIVATE: BATCH FETCH ================

  private Mono<Map<UUID, DocumentEntity>> fetchDocuments(Set<UUID> docIds) {
    if (docIds.isEmpty()) return Mono.just(Map.of());
    return documentRepository
        .findAllById(docIds)
        .collectMap(DocumentEntity::getId, Function.identity());
  }

  private Mono<Map<String, UserBasicResponse>> fetchUsers(Set<String> partyIns) {
    if (partyIns.isEmpty()) return Mono.just(Map.of());
    return userServiceClient.getUsersBasicByIns(partyIns);
  }
}
