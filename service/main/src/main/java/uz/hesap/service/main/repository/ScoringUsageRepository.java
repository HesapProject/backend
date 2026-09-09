package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ScoringUsageRepository extends R2dbcRepository<uz.hesap.service.main.domain.ScoringUsageEntity, UUID> {
  Mono<Long> countByPackageIdAndScoringType(UUID packageId, String scoringType);
}
