package uz.hesap.service.document.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.Utils;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.ProductLedgerStats;
import uz.hesap.service.document.util.Constants;

@Repository
@RequiredArgsConstructor
public class CustomContractProductRepositoryImpl implements CustomContractProductRepository {

  private static final String FROM =
      " FROM "
          + Constants.SCHEMA
          + "."
          + Constants.TABLE_CONTRACT_PRODUCT
          + " cp JOIN "
          + Constants.SCHEMA
          + "."
          + Constants.TABLE_DOCUMENT
          + " d ON d.id = cp.document_id";

  // user UUID → PINFL/STIR (shared DB) — taraflar PINFL'da saqlanadi.
  private static final String USER_PINFL =
      "(SELECT COALESCE(u.pinfl, u.tin) FROM \"user\".\"user\" u WHERE u.id = :userId)";

  private final DatabaseClient db;

  @Override
  public Flux<ProductLedgerRow> findLedger(
      UUID userId,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search,
      Pageable pageable) {
    StringBuilder sql =
        new StringBuilder(
                "SELECT cp.id, cp.document_id, cp.name, cp.amount, cp.field_values,"
                    + " cp.created_date, d.number, d.status, d.currency, d.delivery_at,"
                    + " d.buyer_in, d.seller_in")
            .append(FROM);
    appendFilters(sql, direction, statuses, startDate, endDate, search);
    // "amal sanasi" — delivery_at bo'lsa shu, bo'lmasa hujjat yaratilgan sana
    sql.append(" ORDER BY COALESCE(d.delivery_at, d.created_date) DESC, cp.created_date DESC");
    sql.append(" LIMIT :limit OFFSET :offset");

    var spec =
        bindFilters(db.sql(sql.toString()), userId, statuses, startDate, endDate, search)
            .bind("limit", pageable.getPageSize())
            .bind("offset", pageable.getOffset());

    return spec.map((row, m) -> mapRow(row)).all();
  }

  @Override
  public Mono<Long> countLedger(
      UUID userId,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt").append(FROM);
    appendFilters(sql, direction, statuses, startDate, endDate, search);

    var spec = bindFilters(db.sql(sql.toString()), userId, statuses, startDate, endDate, search);

    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
  }

  @Override
  public Mono<ProductLedgerStats> getLedgerStats(
      UUID userId,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search) {
    StringBuilder sql =
        new StringBuilder("SELECT")
            .append(" COUNT(*) FILTER (WHERE d.buyer_in = ").append(USER_PINFL).append(") AS income_count,")
            .append(" COALESCE(SUM(cp.amount) FILTER (WHERE d.buyer_in = ").append(USER_PINFL).append("), 0) AS income_amount,")
            .append(" COUNT(*) FILTER (WHERE d.seller_in = ").append(USER_PINFL).append(") AS outcome_count,")
            .append(" COALESCE(SUM(cp.amount) FILTER (WHERE d.seller_in = ").append(USER_PINFL).append("), 0) AS outcome_amount")
            .append(FROM);
    appendFilters(sql, null, statuses, startDate, endDate, search);

    var spec = bindFilters(db.sql(sql.toString()), userId, statuses, startDate, endDate, search);

    return spec.map(
            (row, m) ->
                new ProductLedgerStats(
                    row.get("income_count", Long.class),
                    row.get("income_amount", Double.class),
                    row.get("outcome_count", Long.class),
                    row.get("outcome_amount", Double.class)))
        .one()
        .defaultIfEmpty(new ProductLedgerStats(0, 0, 0, 0));
  }

  // ================ SHARED HELPERS ================

  // umumiy WHERE shartlari. direction null → ikkala yo'nalish;
  // statuses bo'sh → bekor qilingan/rad etilganlar chiqarilmaydi
  private void appendFilters(
      StringBuilder sql,
      Direction direction,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search) {
    sql.append(" WHERE cp.deleted = false AND d.deleted = false");
    if (direction == Direction.INCOME) {
      sql.append(" AND d.buyer_in = ").append(USER_PINFL);
    } else if (direction == Direction.OUTCOME) {
      sql.append(" AND d.seller_in = ").append(USER_PINFL);
    } else {
      sql.append(" AND (d.buyer_in = ").append(USER_PINFL).append(" OR d.seller_in = ").append(USER_PINFL).append(")");
    }
    if (statuses != null && !statuses.isEmpty()) {
      sql.append(" AND d.status IN (:statuses)");
    } else {
      sql.append(" AND d.status NOT IN ('REJECTED', 'CANCELLED')");
    }
    if (startDate != null) {
      sql.append(" AND COALESCE(d.delivery_at, d.created_date) >= :startDate");
    }
    if (endDate != null) {
      sql.append(" AND COALESCE(d.delivery_at, d.created_date) <= :endDate");
    }
    if (search != null && !search.isBlank()) {
      sql.append(" AND (cp.name ILIKE :search OR d.number ILIKE :search)");
    }
  }

  // umumiy parametr bind
  private DatabaseClient.GenericExecuteSpec bindFilters(
      DatabaseClient.GenericExecuteSpec spec,
      UUID userId,
      List<DocumentStatus> statuses,
      Instant startDate,
      Instant endDate,
      String search) {
    spec = spec.bind("userId", userId);
    if (statuses != null && !statuses.isEmpty()) {
      spec = spec.bind("statuses", Utils.nameEnums(statuses));
    }
    if (startDate != null) spec = spec.bind("startDate", startDate);
    if (endDate != null) spec = spec.bind("endDate", endDate);
    if (search != null && !search.isBlank()) {
      spec = spec.bind("search", "%" + search.trim() + "%");
    }
    return spec;
  }

  // ================ ROW MAPPING ================

  private ProductLedgerRow mapRow(io.r2dbc.spi.Row row) {
    String status = row.get("status", String.class);
    String currency = row.get("currency", String.class);
    return new ProductLedgerRow(
        row.get("id", UUID.class),
        row.get("document_id", UUID.class),
        row.get("name", String.class),
        row.get("amount", Double.class),
        row.get("field_values", String.class),
        row.get("created_date", Instant.class),
        row.get("number", String.class),
        status != null ? DocumentStatus.valueOf(status) : null,
        currency != null ? Currency.valueOf(currency) : null,
        row.get("delivery_at", Instant.class),
        row.get("buyer_in", String.class),
        row.get("seller_in", String.class));
  }
}
