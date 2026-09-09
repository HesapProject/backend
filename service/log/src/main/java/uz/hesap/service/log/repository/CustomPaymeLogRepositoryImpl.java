package uz.hesap.service.log.repository;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.PaymeLogEntity;

@Repository
@RequiredArgsConstructor
public class CustomPaymeLogRepositoryImpl implements CustomPaymeLogRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public Flux<PaymeLogEntity> findByFilter(
      UUID userId, UUID companyId, Instant startDate, Instant endDate, Pageable pageable) {
    // Chaqiruvchi sort bermasa — eng yangi to'lov birinchi (createdDate DESC).
    Pageable sorted =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdDate")));
    Query query = Query.query(buildCriteria(userId, companyId, startDate, endDate)).with(sorted);
    return r2dbcEntityTemplate.select(query, PaymeLogEntity.class);
  }

  @Override
  public Mono<Long> countByFilter(UUID userId, UUID companyId, Instant startDate, Instant endDate) {
    Query query = Query.query(buildCriteria(userId, companyId, startDate, endDate));
    return r2dbcEntityTemplate.count(query, PaymeLogEntity.class);
  }

  private Criteria buildCriteria(UUID userId, UUID companyId, Instant startDate, Instant endDate) {
    Criteria criteria = Criteria.empty();
    if (userId != null) {
      criteria = criteria.and("user_id").is(userId);
    }
    if (companyId != null) {
      criteria = criteria.and("company_id").is(companyId);
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
