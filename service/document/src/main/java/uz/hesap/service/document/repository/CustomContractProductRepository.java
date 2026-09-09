package uz.hesap.service.document.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.ProductLedgerStats;

// Oldi-berdi: contract_product × document join so'rovlari.
public interface CustomContractProductRepository {

  // Bitta ledger qatori — mahsulot + hujjat ustunlari (join natijasi).
  record ProductLedgerRow(
      UUID id,
      UUID documentId,
      String name,
      Double amount,
      String fieldValues,
      Instant createdDate,
      String documentNumber,
      DocumentStatus documentStatus,
      Currency currency,
      Instant deliveryAt,
      String buyerIn,
      String sellerIn) {}

  Flux<ProductLedgerRow> findLedger(
      UUID userId,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search,
      Pageable pageable);

  Mono<Long> countLedger(
      UUID userId,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search);

  Mono<ProductLedgerStats> getLedgerStats(
      UUID userId, List<DocumentStatus> statuses, Instant startDate, Instant endDate, String search);
}
