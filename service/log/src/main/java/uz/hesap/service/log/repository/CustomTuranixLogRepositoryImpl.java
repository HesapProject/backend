package uz.hesap.service.log.repository;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.TuranixLogEntity;

@Repository
@RequiredArgsConstructor
public class CustomTuranixLogRepositoryImpl implements CustomTuranixLogRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public Flux<TuranixLogEntity> findByFilter(
      UUID userId, String msisdn, Instant startDate, Instant endDate, Pageable pageable) {
    Query query = Query.query(buildCriteria(userId, msisdn, startDate, endDate)).with(pageable);
    return r2dbcEntityTemplate.select(query, TuranixLogEntity.class);
  }

  @Override
  public Mono<Long> countByFilter(
      UUID userId, String msisdn, Instant startDate, Instant endDate) {
    Query query = Query.query(buildCriteria(userId, msisdn, startDate, endDate));
    return r2dbcEntityTemplate.count(query, TuranixLogEntity.class);
  }

  private Criteria buildCriteria(
      UUID userId, String msisdn, Instant startDate, Instant endDate) {
    Criteria criteria = Criteria.empty();
    if (userId != null) {
      criteria = criteria.and("user_id").is(userId);
    }
    if (msisdn != null && !msisdn.isBlank()) {
      criteria = criteria.and("msisdn").is(msisdn);
    }
    if (startDate != null) {
      criteria = criteria.and("created_date").greaterThanOrEquals(startDate);
    }
    if (endDate != null) {
      criteria = criteria.and("created_date").lessThanOrEquals(endDate);
    }
    return criteria;
  }
}
