package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;

public interface PaymentScheduleRequestRepository
    extends R2dbcRepository<PaymentScheduleRequestEntity, UUID> {
  Flux<PaymentScheduleRequestEntity> findAllByContractIdAndDeletedFalse(final UUID contractId);

  Mono<PaymentScheduleRequestEntity> findByIdAndDeletedFalse(final UUID id);

  Flux<PaymentScheduleRequestEntity> findAllByPaymentIdAndDeletedFalse(final UUID paymentId);

  // Bitta to'lov bo'yicha faqat bitta faol (PENDING) to'lov so'rovi bo'ladi.
  Mono<Boolean> existsByPaymentIdAndStatusAndDeletedFalse(
      final UUID paymentId,
      final uz.hesap.service.document.domain.enums.PaymentScheduleStatus status);
}
