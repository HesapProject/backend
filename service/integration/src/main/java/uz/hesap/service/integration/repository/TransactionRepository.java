package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.TransactionEntity;

public interface TransactionRepository extends R2dbcRepository<TransactionEntity, UUID> {

  // Foydalanuvchining tranzaksiyalari — eng oxirgisi eng tepada (timestamp DESC).
  Flux<TransactionEntity> findAllByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

  Mono<Long> countByUserId(UUID userId);
}
