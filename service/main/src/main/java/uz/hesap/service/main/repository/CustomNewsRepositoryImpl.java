package uz.hesap.service.main.repository;

import static org.springframework.data.relational.core.query.Criteria.where;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.NewsEntity;

@Repository
@RequiredArgsConstructor
public class CustomNewsRepositoryImpl implements CustomNewsRepository {

  private final R2dbcEntityTemplate entityTemplate;

  @Override
  public Mono<Page<NewsEntity>> findNews(String search, Boolean home, Pageable pageable) {
    Criteria criteria = where("is_deleted").is(Boolean.FALSE);

    if (search != null && !search.isBlank()) {
      String s = "%" + search.toLowerCase() + "%";
      criteria =
          criteria.and(
              Criteria.from(
                  where("title_uz").like(s), where("title_ru").like(s), where("title_en").like(s)));
    }

    if (home != null) {
      criteria = criteria.and("is_home").is(home);
    }

    Query query = Query.query(criteria).with(pageable);

    Mono<List<NewsEntity>> contentMono =
        entityTemplate.select(NewsEntity.class).matching(query).all().collectList();

    Mono<Long> countMono = entityTemplate.count(Query.query(criteria), NewsEntity.class);

    return Mono.zip(contentMono, countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
