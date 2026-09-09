package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PaymentOrderEntity;

public interface PaymentOrderRepository extends R2dbcRepository<PaymentOrderEntity, UUID> {

  // Webhook to'lovni yakunlaganda — shu to'lovchining eng so'nggi kutilayotgan orderi.
  Mono<PaymentOrderEntity> findFirstByUniqueIdAndStatusOrderByCreatedDateDesc(
      UUID uniqueId, String status);
}
