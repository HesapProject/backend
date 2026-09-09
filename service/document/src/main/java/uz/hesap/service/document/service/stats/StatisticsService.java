package uz.hesap.service.document.service.stats;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.ProductLedgerStats;
import uz.hesap.service.document.repository.CustomDocumentRepository;
import uz.hesap.service.document.repository.CustomPaymentScheduleRepository;
import uz.hesap.service.document.repository.CustomPaymentScheduleRepository.FlowStats;
import uz.hesap.service.document.service.c2c.ProductsService;

/** Biznes statistikasi — vendor dashboard uchun barcha agregatlarni bitta javobga yig'adi. */
@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final CustomDocumentRepository customDocumentRepository;
  private final CustomPaymentScheduleRepository customPaymentScheduleRepository;
  private final ProductsService productsService;

  // Mahsulot (oldi-berdi) statistikasi — StatisticsController doirasida bo'lishi uchun delegatsiya.
  public Mono<ProductLedgerStats> getProductStats(
      UUID userId, List<DocumentStatus> statuses, Instant startDate, Instant endDate, String search) {
    return productsService.getStats(userId, statuses, startDate, endDate, search);
  }

  /**
   * @param userId aktiv entity (to'lov flow stats uchun)
   * @param in taraf identifikatori (PINFL/STIR) — shartnoma stats uchun (user o'chsa ham)
   */
  public Mono<BusinessStatsResponse> getBusinessStats(UUID userId, String in) {
    var statusCountsMono =
        customDocumentRepository
            .countByStatus(in)
            .collectMap(
                CustomDocumentRepository.DocumentStatusCount::status,
                CustomDocumentRepository.DocumentStatusCount::count);

    var activeContractsMono =
        customDocumentRepository
            .getActiveStatsByCurrency(in)
            .sort(Comparator.comparing(s -> s.currency().name()))
            .collectList();

    var flowStatsMono = customPaymentScheduleRepository.getFlowStats(userId).collectList();

    var cashflowMono = customPaymentScheduleRepository.getMonthlyCashflow(userId).collectList();

    return Mono.zip(statusCountsMono, activeContractsMono, flowStatsMono, cashflowMono)
        .map(
            tuple ->
                new BusinessStatsResponse(
                    tuple.getT1(),
                    tuple.getT2(),
                    byDirection(tuple.getT3(), Direction.INCOME),
                    byDirection(tuple.getT3(), Direction.OUTCOME),
                    tuple.getT4()));
  }

  // INCOME → receivables (bizga to'lashadi), OUTCOME → payables (biz to'laymiz)
  private List<BusinessStatsResponse.PaymentFlowStats> byDirection(
      List<FlowStats> flows, Direction direction) {
    return flows.stream()
        .filter(f -> f.direction() == direction)
        .map(FlowStats::stats)
        .sorted(Comparator.comparing(s -> s.currency().name()))
        .toList();
  }
}
