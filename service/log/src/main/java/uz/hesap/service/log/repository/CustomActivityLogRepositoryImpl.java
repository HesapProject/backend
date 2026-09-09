package uz.hesap.service.log.repository;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.ActivityLogEntity;

@Repository
@RequiredArgsConstructor
public class CustomActivityLogRepositoryImpl implements CustomActivityLogRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public Flux<ActivityLogEntity> findByFilter(
      String actorIn, String companyIn, String activity, Instant startDate, Instant endDate,
      Pageable pageable) {
    Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "created_date");
    Query query =
        Query.query(buildCriteria(actorIn, companyIn, activity, startDate, endDate))
            .with(pageable)
            .sort(sort);
    return r2dbcEntityTemplate.select(query, ActivityLogEntity.class);
  }

  @Override
  public Mono<Long> countByFilter(
      String actorIn, String companyIn, String activity, Instant startDate, Instant endDate) {
    Query query = Query.query(buildCriteria(actorIn, companyIn, activity, startDate, endDate));
    return r2dbcEntityTemplate.count(query, ActivityLogEntity.class);
  }

  private Criteria buildCriteria(
      String actorIn, String companyIn, String activity, Instant startDate, Instant endDate) {
    Criteria criteria = Criteria.empty();
    if (actorIn != null && !actorIn.isBlank()) {
      criteria = criteria.and("actor_in").is(actorIn);
    }
    if (companyIn != null && !companyIn.isBlank()) {
      criteria = criteria.and("company_in").is(companyIn);
    }
    if (activity != null && !activity.isBlank()) {
      criteria = criteria.and("activity").is(activity);
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
