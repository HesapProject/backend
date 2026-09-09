package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.integration.domain.PochtaLogEntity;

public interface PochtaLogRepository extends ReactiveCrudRepository<PochtaLogEntity, UUID> {
  Flux<PochtaLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
