package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.integration.domain.AmoLogEntity;

public interface AmoLogRepository extends ReactiveCrudRepository<AmoLogEntity, UUID> {
  Flux<AmoLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
