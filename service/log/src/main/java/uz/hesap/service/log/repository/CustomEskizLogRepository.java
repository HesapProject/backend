package uz.hesap.service.log.repository;

import java.time.Instant;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.EskizLogEntity;

public interface CustomEskizLogRepository {
  Flux<EskizLogEntity> findByFilter(
      Boolean isFailed, Instant fromDate, Instant toDate, Pageable pageable);

  Mono<Long> countByFilter(Boolean isFailed, Instant fromDate, Instant toDate);
}
