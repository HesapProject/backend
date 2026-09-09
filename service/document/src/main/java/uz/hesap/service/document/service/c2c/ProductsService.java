package uz.hesap.service.document.service.c2c;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.ProductLedgerResponse;
import uz.hesap.service.document.model.response.ProductLedgerStats;
import uz.hesap.service.document.repository.CustomContractProductRepository;
import uz.hesap.service.document.repository.CustomContractProductRepository.ProductLedgerRow;
import uz.hesap.service.document.webclient.UserServiceClient;

// Oldi-berdi: shartnomalardagi mahsulotlarni kiruvchi (buyer) / chiquvchi (seller)
// yo'nalishda, kontragent ma'lumoti bilan qaytaradi.
@Service
@RequiredArgsConstructor
@Log4j2
public class ProductsService {

  private final CustomContractProductRepository customContractProductRepository;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper;

  public Mono<Page<ProductLedgerResponse>> getLedger(
      UUID userId,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search,
      Pageable pageable) {
    // Taraflar PINFL'da — joriy user PINFL'ini bir marta resolish qilamiz (counterpart/direction).
    return userServiceClient
        .getUserById(userId)
        .flatMap(
            me -> {
              String userIn = me.in();
              return customContractProductRepository
                  .findLedger(userId, direction, statuses, startDate, endDate, search, pageable)
                  .collectList()
                  .flatMap(
                      rows -> {
                        if (rows.isEmpty()) {
                          return Mono.just(
                              new PageImpl<ProductLedgerResponse>(List.of(), pageable, 0));
                        }
                        List<String> counterpartIns =
                            rows.stream()
                                .map(r -> counterpartIn(r, userIn))
                                .filter(s -> s != null && !s.isBlank())
                                .distinct()
                                .toList();
                        return findUsers(counterpartIns)
                            .flatMap(
                                usersMap -> {
                                  List<ProductLedgerResponse> items =
                                      rows.stream()
                                          .map(r -> toResponse(r, userIn, usersMap))
                                          .toList();
                                  return customContractProductRepository
                                      .countLedger(
                                          userId, direction, statuses, startDate, endDate, search)
                                      .map(count -> new PageImpl<>(items, pageable, count));
                                });
                      });
            });
  }

  public Mono<ProductLedgerStats> getStats(
      UUID userId, List<DocumentStatus> statuses, Instant startDate, Instant endDate, String search) {
    return customContractProductRepository.getLedgerStats(
        userId, statuses, startDate, endDate, search);
  }

  // kontragent PINFL: user buyer bo'lsa — seller, aks holda buyer
  private String counterpartIn(ProductLedgerRow row, String userIn) {
    return userIn != null && userIn.equals(row.buyerIn()) ? row.sellerIn() : row.buyerIn();
  }

  // main-service xatosi ledger'ni yiqitmasin — kontragent'siz qaytadi (PINFL bo'yicha)
  private Mono<Map<String, UserResponse>> findUsers(List<String> ins) {
    return userServiceClient
        .getUsersByIns(ins)
        .onErrorResume(
            e -> {
              log.warn("Ledger counterpart enrichment failed: {}", e.getMessage());
              return Mono.just(Map.of());
            });
  }

  private ProductLedgerResponse toResponse(
      ProductLedgerRow row, String userIn, Map<String, UserResponse> usersMap) {
    Direction direction =
        userIn != null && userIn.equals(row.buyerIn()) ? Direction.INCOME : Direction.OUTCOME;
    return new ProductLedgerResponse(
        row.id(),
        row.documentId(),
        row.documentNumber(),
        row.documentStatus(),
        direction,
        usersMap.get(counterpartIn(row, userIn)),
        row.name(),
        row.amount(),
        parseValues(row.fieldValues()),
        row.currency(),
        row.deliveryAt(),
        row.createdDate());
  }

  // field_values JSON string → Map (parse bo'lmasa null)
  private Map<String, Object> parseValues(String fieldValues) {
    if (fieldValues == null || fieldValues.isBlank()) return null;
    try {
      return objectMapper.readValue(fieldValues, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException ex) {
      log.warn("Contract product values JSON parse failed: {}", ex.getMessage());
      return null;
    }
  }
}
