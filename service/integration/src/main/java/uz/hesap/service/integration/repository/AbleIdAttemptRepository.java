package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.AbleIdAttemptEntity;

public interface AbleIdAttemptRepository extends R2dbcRepository<AbleIdAttemptEntity, UUID> {
  Mono<AbleIdAttemptEntity> findByAttemptId(String attemptId);
}
