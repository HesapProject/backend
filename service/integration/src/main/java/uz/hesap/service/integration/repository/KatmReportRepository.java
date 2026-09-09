package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.integration.domain.KatmReportEntity;
import uz.hesap.service.integration.domain.enums.KatmReportStatus;

public interface KatmReportRepository extends ReactiveCrudRepository<KatmReportEntity, UUID> {

  Flux<KatmReportEntity> findAllByStatus(KatmReportStatus status);

  Flux<KatmReportEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Flux<KatmReportEntity> findAllByPinflOrderByCreatedAtDesc(String pinfl, Pageable pageable);

  Flux<KatmReportEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
