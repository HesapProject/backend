package uz.hesap.service.document.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.NoticeEntity;

@Repository
@RequiredArgsConstructor
public class CustomNoticeRepositoryImpl implements CustomNoticeRepository {

  private final R2dbcEntityTemplate template;

  // notices endi buyer_in/seller_in (PINFL) to'g'ridan-to'g'ri saqlaydi — JOIN kerak emas.
  // Talabnoma: yuboruvchi = seller (haqdor), qabul qiluvchi = buyer (qarzdor).
  private static final String BASE = "SELECT %s FROM document.notices c WHERE c.deleted = false";

  private static String where(String fromIn, String toIn) {
    StringBuilder sb = new StringBuilder();
    if (fromIn != null && !fromIn.isBlank()) sb.append(" AND c.seller_in = :fromIn");
    if (toIn != null && !toIn.isBlank()) sb.append(" AND c.buyer_in = :toIn");
    return sb.toString();
  }

  private static DatabaseClient.GenericExecuteSpec bind(
      DatabaseClient.GenericExecuteSpec spec, String fromIn, String toIn) {
    if (fromIn != null && !fromIn.isBlank()) spec = spec.bind("fromIn", fromIn);
    if (toIn != null && !toIn.isBlank()) spec = spec.bind("toIn", toIn);
    return spec;
  }

  @Override
  public Flux<NoticeEntity> findFiltered(String fromIn, String toIn, int size, long offset) {
    String sql =
        String.format(BASE, "c.*")
            + where(fromIn, toIn)
            + " ORDER BY c.created_date DESC LIMIT :size OFFSET :offset";
    DatabaseClient.GenericExecuteSpec spec =
        bind(template.getDatabaseClient().sql(sql), fromIn, toIn)
            .bind("size", size)
            .bind("offset", offset);
    return spec.map((row, meta) -> template.getConverter().read(NoticeEntity.class, row, meta))
        .all();
  }

  @Override
  public Mono<Long> countFiltered(String fromIn, String toIn) {
    String sql = String.format(BASE, "COUNT(*) AS cnt") + where(fromIn, toIn);
    return bind(template.getDatabaseClient().sql(sql), fromIn, toIn)
        .map((row, meta) -> row.get("cnt", Long.class))
        .one()
        .defaultIfEmpty(0L);
  }
}
