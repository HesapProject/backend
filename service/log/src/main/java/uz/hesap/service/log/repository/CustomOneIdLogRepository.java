package uz.hesap.service.log.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.OneIdLogEntity;

public interface CustomOneIdLogRepository {
  Flux<OneIdLogEntity> findByFilter(
      UUID companyId, UUID userId, Instant startDate, Instant endDate, Pageable pageable);

  Mono<Long> countByFilter(UUID companyId, UUID userId, Instant startDate, Instant endDate);
}
