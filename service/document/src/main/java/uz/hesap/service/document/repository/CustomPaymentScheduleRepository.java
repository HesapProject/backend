package uz.hesap.service.document.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentFilterStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.model.response.B2BPaymentStatsResponse;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.PaymentScoreResponse;

public interface CustomPaymentScheduleRepository {

  Mono<PaymentScoreResponse> getScore(UUID userId);

  // ---- userId bo'yicha filter ----
  Flux<PaymentEntity> findFiltered(
      UUID userId,
      Direction direction,
      List<PaymentScheduleStatus> statuses,
      Instant startDate,
      Instant endDate,
      Pageable pageable);

  Mono<Long> countFiltered(
      UUID userId,
      Direction direction,
      List<PaymentScheduleStatus> statuses,
      Instant startDate,
      Instant endDate);

  // ---- /payments admin filter: sana + buyerIn/sellerIn (hujjat) + contractId + statuses ----
  Flux<PaymentEntity> findByAdminFilter(
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses,
      Pageable pageable);

  Mono<Long> countByAdminFilter(
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses);

  // ---- PaidSchedule: userId bo'yicha ----
  Flux<PaidScheduleEntity> findPaidByUserFiltered(
      UUID userId, Direction direction, Pageable pageable);

  Mono<Long> countPaidByUserFiltered(UUID userId, Direction direction);

  // ---- Stats: userId bo'yicha (currency bo'yicha) ----
  Flux<B2BPaymentStatsResponse> getStats(UUID userId);

  // ---- Biznes statistika: direction + currency kesimida pending/overdue/paid ----
  Flux<FlowStats> getFlowStats(UUID userId);

  record FlowStats(Direction direction, BusinessStatsResponse.PaymentFlowStats stats) {}

  // ---- Oylik pul oqimi: o'tgan oylar PAID + kelgusi oylar PENDING ----
  Flux<BusinessStatsResponse.CashflowPoint> getMonthlyCashflow(UUID userId);
}
