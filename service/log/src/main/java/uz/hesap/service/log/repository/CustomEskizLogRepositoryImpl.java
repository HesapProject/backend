package uz.hesap.service.log.repository;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.EskizLogEntity;

@Repository
@RequiredArgsConstructor
public class CustomEskizLogRepositoryImpl implements CustomEskizLogRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public Flux<EskizLogEntity> findByFilter(
      Boolean isFailed, Instant fromDate, Instant toDate, Pageable pageable) {
    Query query = Query.query(buildCriteria(isFailed, fromDate, toDate)).with(pageable);
    return r2dbcEntityTemplate.select(query, EskizLogEntity.class);
  }

  @Override
  public Mono<Long> countByFilter(Boolean isFailed, Instant fromDate, Instant toDate) {
    Query query = Query.query(buildCriteria(isFailed, fromDate, toDate));
    return r2dbcEntityTemplate.count(query, EskizLogEntity.class);
  }

  private Criteria buildCriteria(Boolean isFailed, Instant fromDate, Instant toDate) {
    Criteria criteria = Criteria.empty();
    if (isFailed != null) {
      criteria = criteria.and("is_failed").is(isFailed);
    }
    if (fromDate != null) {
      criteria = criteria.and("timestamp").greaterThanOrEquals(fromDate);
    }
    if (toDate != null) {
      criteria = criteria.and("timestamp").lessThanOrEquals(toDate);
    }
    return criteria;
  }
}
