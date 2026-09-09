package uz.hesap.service.log.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.RoumingLogEntity;

@Repository
public interface RoumingLogRepository extends R2dbcRepository<RoumingLogEntity, UUID> {

  Flux<RoumingLogEntity> findAllByCreatedDateBetweenOrderByCreatedDateDesc(
      Instant startDate, Instant endDate, Pageable pageable);

  Mono<Long> countByCreatedDateBetween(Instant startDate, Instant endDate);
}
