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
import uz.hesap.service.log.domain.OneIdLogEntity;

@Repository
@RequiredArgsConstructor
public class CustomOneIdLogRepositoryImpl implements CustomOneIdLogRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public Flux<OneIdLogEntity> findByFilter(
      UUID companyId, UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
    Query query = Query.query(buildCriteria(companyId, userId, startDate, endDate)).with(pageable);
    return r2dbcEntityTemplate.select(query, OneIdLogEntity.class);
  }

  @Override
  public Mono<Long> countByFilter(UUID companyId, UUID userId, Instant startDate, Instant endDate) {
    Query query = Query.query(buildCriteria(companyId, userId, startDate, endDate));
    return r2dbcEntityTemplate.count(query, OneIdLogEntity.class);
  }

  private Criteria buildCriteria(UUID companyId, UUID userId, Instant startDate, Instant endDate) {
    Criteria criteria = Criteria.empty();
    if (companyId != null) {
      criteria = criteria.and("company_id").is(companyId);
    }
    if (userId != null) {
      criteria = criteria.and("user_id").is(userId);
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
