package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.payment.DelayRequestEntity;

public interface DelayRequestRepository extends R2dbcRepository<DelayRequestEntity, UUID> {
  Mono<DelayRequestEntity> findByIdAndDeletedFalse(final UUID id);

  // Bitta to'lov bo'yicha faqat bitta faol (PENDING) kechiktirish so'rovi bo'ladi.
  Mono<Boolean> existsByPaymentIdAndStatusAndDeletedFalse(
      final UUID paymentId,
      final uz.hesap.service.document.domain.enums.PaymentScheduleStatus status);
}
