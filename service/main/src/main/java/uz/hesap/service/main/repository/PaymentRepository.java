package uz.hesap.service.main.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.PaymentEntity;

@Repository
public interface PaymentRepository extends R2dbcRepository<PaymentEntity, UUID> {

  Flux<PaymentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Flux<PaymentEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Mono<Long> countByUserId(UUID userId);

  // Balans hisoblash: barcha to'lovlar (kiruvchi) yig'indisi.
  @Query("SELECT COALESCE(SUM(amount), 0) FROM \"user\".payments WHERE user_id = :userId")
  Mono<Double> sumAmountByUserId(UUID userId);

  // Sana oralig'idagi kiruvchi yig'indi (xulosa uchun; null sanalar — cheklovsiz).
  @Query(
      "SELECT COALESCE(SUM(amount), 0) FROM \"user\".payments WHERE user_id = :userId "
          + "AND (:from IS NULL OR created_at >= :from) AND (:to IS NULL OR created_at <= :to)")
  Mono<Double> sumAmountByUserIdBetween(UUID userId, Instant from, Instant to);
}
