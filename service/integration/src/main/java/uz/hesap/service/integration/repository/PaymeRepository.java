package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PaymeTransactionEntity;

public interface PaymeRepository extends R2dbcRepository<PaymeTransactionEntity, UUID> {
  Flux<PaymeTransactionEntity> findAllByPaycomTimeBetween(Long from, Long to);

  Mono<PaymeTransactionEntity> findByPaycomId(String paycomId);
}
