package uz.hesap.service.log.repository;

import java.time.Instant;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.ActivityLogEntity;

public interface CustomActivityLogRepository {
  Flux<ActivityLogEntity> findByFilter(
      String actorIn, String companyIn, String activity, Instant startDate, Instant endDate,
      Pageable pageable);

  Mono<Long> countByFilter(
      String actorIn, String companyIn, String activity, Instant startDate, Instant endDate);
}
