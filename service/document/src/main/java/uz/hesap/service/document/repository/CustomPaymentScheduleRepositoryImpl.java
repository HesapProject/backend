package uz.hesap.service.document.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.Utils;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.PaymentFilterStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.model.response.B2BPaymentStatsResponse;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.PaymentScoreResponse;
import uz.hesap.service.document.util.Constants;

@Repository
@RequiredArgsConstructor
public class CustomPaymentScheduleRepositoryImpl implements CustomPaymentScheduleRepository {

  private static final String PS_TABLE = Constants.SCHEMA + "." + Constants.TABLE_PAYMENT_SCHEDULE;
  private static final String PAID_TABLE = Constants.SCHEMA + "." + Constants.TABLE_PAID_SCHEDULE;
  private static final String DOC_TABLE = Constants.SCHEMA + "." + Constants.TABLE_DOCUMENT;

  // Taraflar PINFL'da — user UUID'ni shared DB'dan PINFL/STIR'ga aylantiruvchi scalar subquery
  // (uncorrelated → bir marta hisoblanadi). :userId yoki :ownerId bind'iga mos keladi.
  private static final String USER_PINFL =
      "(SELECT COALESCE(u.pinfl, u.tin) FROM \"user\".\"user\" u WHERE u.id = :userId)";
  private static final String OWNER_PINFL =
      "(SELECT COALESCE(u.pinfl, u.tin) FROM \"user\".\"user\" u WHERE u.id = :ownerId)";

  private final DatabaseClient db;
  private final R2dbcConverter converter;

  // ================ score ================

  @Override
  public Mono<PaymentScoreResponse> getScore(UUID userId) {
    String sql =
        """
            SELECT COUNT(*) FILTER (
                WHERE paid_at IS NULL
                    AND contract_payment_date < NOW()
                    AND deleted = false
                )        AS unpaid,

                   COUNT(*) FILTER (
                       WHERE paid_at < contract_payment_date
                           AND deleted = false
                       ) AS early_paid,

                   COUNT(*) FILTER (
                       WHERE date_trunc('day', paid_at) = date_trunc('day', contract_payment_date)
                           AND deleted = false
                       ) AS on_time,

                   COUNT(*) FILTER (
                       WHERE paid_at > contract_payment_date
                           AND paid_at <= contract_payment_date + INTERVAL '30 days'
                           AND deleted = false
                       ) AS late,

                   COUNT(*) FILTER (
                       WHERE paid_at > contract_payment_date + INTERVAL '30 days'
                           AND deleted = false
                       ) AS very_late,

                   COUNT(*) FILTER (
                       WHERE changed_payment_date is not null
                       ) AS extended
            FROM \n
            """
            + PS_TABLE
            + """
            \s
            WHERE contract_id IN (SELECT id FROM document.contracts
                                  WHERE buyer_in = (SELECT COALESCE(u.pinfl, u.tin)
                                                    FROM "user"."user" u WHERE u.id = :userId)
                                    AND deleted = false)
            """;

    return db.sql(sql)
        .bind("userId", userId)
        .map(
            (row, metadata) ->
                new PaymentScoreResponse(
                    row.get("unpaid", Long.class),
                    row.get("early_paid", Long.class),
                    row.get("on_time", Long.class),
                    row.get("late", Long.class),
                    row.get("very_late", Long.class),
                    row.get("extended", Long.class)))
        .one();
  }

  // ================ PaymentSchedule ================

  @Override
  public Flux<PaymentEntity> findFiltered(
      UUID userId,
      Direction direction,
      List<PaymentScheduleStatus> statuses,
      Instant startDate,
      Instant endDate,
      Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(PS_TABLE);
    sql.append(" WHERE deleted = false");
    appendOwnerFilter(sql, direction, "contract_id");
    appendStatusFilter(sql, statuses);
    appendDateFilter(sql, startDate, endDate);
    appendActiveDocumentFilter(sql);
    sql.append(" ORDER BY contract_payment_date ASC");
    sql.append(" LIMIT :limit OFFSET :offset");

    var spec = bindStatusFilter(db.sql(sql.toString()).bind("ownerId", userId), statuses);
    spec = bindDateFilter(spec, startDate, endDate);
    spec = spec.bind("limit", pageable.getPageSize()).bind("offset", pageable.getOffset());

    return spec.map((row, m) -> converter.read(PaymentEntity.class, row, m)).all();
  }

  @Override
  public Mono<Long> countFiltered(
      UUID userId,
      Direction direction,
      List<PaymentScheduleStatus> statuses,
      Instant startDate,
      Instant endDate) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM ").append(PS_TABLE);
    sql.append(" WHERE deleted = false");
    appendOwnerFilter(sql, direction, "contract_id");
    appendStatusFilter(sql, statuses);
    appendDateFilter(sql, startDate, endDate);
    appendActiveDocumentFilter(sql);

    var spec = bindStatusFilter(db.sql(sql.toString()).bind("ownerId", userId), statuses);
    spec = bindDateFilter(spec, startDate, endDate);
    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
  }

  // ================ /payments admin filter ================

  @Override
  public Flux<PaymentEntity> findByAdminFilter(
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses,
      Pageable pageable) {
    StringBuilder sql =
        new StringBuilder("SELECT ps.* FROM ")
            .append(PS_TABLE)
            .append(" ps JOIN ")
            .append(DOC_TABLE)
            .append(" d ON d.id = ps.contract_id WHERE ps.deleted = false AND d.deleted = false");
    appendAdminWhere(sql, startDate, endDate, buyerIn, sellerIn, contractId, statuses, contractStatuses);
    sql.append(" ORDER BY ps.contract_payment_date ASC LIMIT :limit OFFSET :offset");

    var spec = bindAdmin(db.sql(sql.toString()), startDate, endDate, buyerIn, sellerIn, contractId);
    spec = spec.bind("limit", pageable.getPageSize()).bind("offset", pageable.getOffset());
    return spec.map((row, m) -> converter.read(PaymentEntity.class, row, m)).all();
  }

  @Override
  public Mono<Long> countByAdminFilter(
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses) {
    StringBuilder sql =
        new StringBuilder("SELECT COUNT(*) AS cnt FROM ")
            .append(PS_TABLE)
            .append(" ps JOIN ")
            .append(DOC_TABLE)
            .append(" d ON d.id = ps.contract_id WHERE ps.deleted = false AND d.deleted = false");
    appendAdminWhere(sql, startDate, endDate, buyerIn, sellerIn, contractId, statuses, contractStatuses);
    var spec = bindAdmin(db.sql(sql.toString()), startDate, endDate, buyerIn, sellerIn, contractId);
    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
  }

  private void appendAdminWhere(
      StringBuilder sql,
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId,
      List<PaymentFilterStatus> statuses,
      List<DocumentStatus> contractStatuses) {
    if (startDate != null) sql.append(" AND ps.contract_payment_date >= :startDate");
    if (endDate != null) sql.append(" AND ps.contract_payment_date <= :endDate");
    if (buyerIn != null && !buyerIn.isBlank()) sql.append(" AND d.buyer_in = :buyerIn");
    if (sellerIn != null && !sellerIn.isBlank()) sql.append(" AND d.seller_in = :sellerIn");
    if (contractId != null) sql.append(" AND ps.contract_id = :contractId");
    if (statuses != null && !statuses.isEmpty()) {
      String conds =
          statuses.stream()
              .distinct()
              .map(CustomPaymentScheduleRepositoryImpl::statusCondition)
              .reduce((a, b) -> a + " OR " + b)
              .orElse("TRUE");
      sql.append(" AND (").append(conds).append(")");
    }
    // Shartnoma (hujjat) holati bo'yicha filter — Home faqat aktiv shartnomalar
    // to'lovlarini ko'rsatishi uchun. Qiymatlar enum .name() — inline xavfsiz.
    if (contractStatuses != null && !contractStatuses.isEmpty()) {
      String in =
          contractStatuses.stream()
              .distinct()
              .map(s -> "'" + s.name() + "'")
              .reduce((a, b) -> a + "," + b)
              .orElse("''");
      sql.append(" AND d.status IN (").append(in).append(")");
    }
  }

  private DatabaseClient.GenericExecuteSpec bindAdmin(
      DatabaseClient.GenericExecuteSpec spec,
      Instant startDate,
      Instant endDate,
      String buyerIn,
      String sellerIn,
      UUID contractId) {
    if (startDate != null) spec = spec.bind("startDate", startDate);
    if (endDate != null) spec = spec.bind("endDate", endDate);
    if (buyerIn != null && !buyerIn.isBlank()) spec = spec.bind("buyerIn", buyerIn);
    if (sellerIn != null && !sellerIn.isBlank()) spec = spec.bind("sellerIn", sellerIn);
    if (contractId != null) spec = spec.bind("contractId", contractId);
    return spec;
  }

  // Hisoblangan holat → SQL sharti (ps alias).
  private static String statusCondition(PaymentFilterStatus s) {
    return switch (s) {
      case ACTIVE -> "(ps.status = 'PENDING' AND ps.contract_payment_date < NOW())";
      case PENDING ->
          "(ps.status = 'PENDING' AND COALESCE(ps.paid_amount,0) = 0 AND ps.contract_payment_date >= NOW())";
      case PARTLY ->
          "(ps.status = 'PENDING' AND COALESCE(ps.paid_amount,0) > 0"
              + " AND COALESCE(ps.paid_amount,0) < ps.total_amount)";
      case DONE -> "(ps.status = 'PAID')";
    };
  }

  // ================ PaidSchedule ================

  @Override
  public Flux<PaidScheduleEntity> findPaidByUserFiltered(
      UUID userId, Direction direction, Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(PAID_TABLE);
    sql.append(" WHERE deleted = false");
    appendOwnerFilter(sql, direction, "document_id");
    // payment_transactions hali payment_date ustunini saqlaydi (PaymentEntity'dan farqli).
    sql.append(" ORDER BY payment_date ASC");
    sql.append(" LIMIT :limit OFFSET :offset");

    var spec =
        db.sql(sql.toString())
            .bind("ownerId", userId)
            .bind("limit", pageable.getPageSize())
            .bind("offset", pageable.getOffset());

    return spec.map((row, m) -> converter.read(PaidScheduleEntity.class, row, m)).all();
  }

  @Override
  public Mono<Long> countPaidByUserFiltered(UUID userId, Direction direction) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM ").append(PAID_TABLE);
    sql.append(" WHERE deleted = false");
    appendOwnerFilter(sql, direction, "document_id");

    var spec = db.sql(sql.toString()).bind("ownerId", userId);
    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
  }

  // ================ Stats (currency bo'yicha) ================

  @Override
  public Flux<B2BPaymentStatsResponse> getStats(UUID userId) {
    String sql =
        "SELECT COALESCE(currency, 'UZS') AS currency,"
            + " COUNT(*) AS total,"
            + " COUNT(*) FILTER (WHERE status = 'PENDING') AS pending,"
            + " COUNT(*) FILTER (WHERE status = 'PAID') AS paid"
            + " FROM "
            + PS_TABLE
            + " WHERE deleted = false"
            + " AND contract_id IN (SELECT id FROM document.contracts"
            + "     WHERE (buyer_in = " + USER_PINFL + " OR seller_in = " + USER_PINFL + ")"
            + "       AND deleted = false)"
            + " GROUP BY COALESCE(currency, 'UZS')";

    return db.sql(sql)
        .bind("userId", userId)
        .map(
            (row, m) -> {
              String currStr = row.get("currency", String.class);
              Currency curr = Currency.valueOf(currStr);
              return new B2BPaymentStatsResponse(
                  curr,
                  row.get("total", Long.class),
                  row.get("pending", Long.class),
                  row.get("paid", Long.class));
            })
        .all();
  }

  // ================ Biznes statistika ================

  // Direction + currency kesimida pending/overdue/paid agregatlari.
  // pending/overdue — faqat ACTIVE shartnomalar, paid — ACTIVE+COMPLETED.
  // pending summasi = amount - paid_amount (qisman to'langanlar hisobga olinadi).
  @Override
  public Flux<FlowStats> getFlowStats(UUID userId) {
    String sql =
        "SELECT COALESCE(ps.currency, 'UZS') AS currency,"
            + " CASE WHEN d.seller_in = " + USER_PINFL + " THEN 'INCOME' ELSE 'OUTCOME' END AS direction,"
            + " COUNT(*) FILTER (WHERE ps.status = 'PENDING' AND d.status = 'ACTIVE') AS pending_count,"
            + " COALESCE(SUM(ps.total_amount - COALESCE(ps.paid_amount, 0))"
            + "   FILTER (WHERE ps.status = 'PENDING' AND d.status = 'ACTIVE'), 0) AS pending_amount,"
            + " COUNT(*) FILTER (WHERE ps.status = 'PENDING' AND d.status = 'ACTIVE'"
            + "   AND ps.contract_payment_date < NOW()) AS overdue_count,"
            + " COALESCE(SUM(ps.total_amount - COALESCE(ps.paid_amount, 0))"
            + "   FILTER (WHERE ps.status = 'PENDING' AND d.status = 'ACTIVE'"
            + "   AND ps.contract_payment_date < NOW()), 0) AS overdue_amount,"
            + " COUNT(*) FILTER (WHERE ps.status = 'PAID') AS paid_count,"
            + " COALESCE(SUM(ps.total_amount) FILTER (WHERE ps.status = 'PAID'), 0) AS paid_amount"
            + " FROM "
            + PS_TABLE
            + " ps JOIN "
            + DOC_TABLE
            + " d ON d.id = ps.contract_id AND d.deleted = false"
            + " WHERE ps.deleted = false"
            + " AND d.status IN ('ACTIVE', 'COMPLETED')"
            + " AND (d.buyer_in = " + USER_PINFL + " OR d.seller_in = " + USER_PINFL + ")"
            + " GROUP BY 1, 2";

    return db.sql(sql)
        .bind("userId", userId)
        .map(
            (row, m) ->
                new FlowStats(
                    Direction.valueOf(row.get("direction", String.class)),
                    new BusinessStatsResponse.PaymentFlowStats(
                        Currency.valueOf(row.get("currency", String.class)),
                        row.get("pending_count", Long.class),
                        row.get("pending_amount", Double.class),
                        row.get("overdue_count", Long.class),
                        row.get("overdue_amount", Double.class),
                        row.get("paid_count", Long.class),
                        row.get("paid_amount", Double.class))))
        .all();
  }

  // Oylik pul oqimi: -5..+6 oy oynasida. PAID — paid_at bo'yicha,
  // PENDING (faqat ACTIVE shartnomalar) — contract_payment_date bo'yicha, qolgan summa bilan.
  @Override
  public Flux<BusinessStatsResponse.CashflowPoint> getMonthlyCashflow(UUID userId) {
    String sql =
        "SELECT to_char(month, 'YYYY-MM') AS month, currency, direction, paid,"
            + " SUM(amount) AS amount FROM ("
            + " SELECT date_trunc('month', ps.contract_payment_date) AS month,"
            + " COALESCE(ps.currency, 'UZS') AS currency,"
            + " CASE WHEN d.seller_in = " + USER_PINFL + " THEN 'INCOME' ELSE 'OUTCOME' END AS direction,"
            + " false AS paid,"
            + " ps.total_amount - COALESCE(ps.paid_amount, 0) AS amount"
            + " FROM "
            + PS_TABLE
            + " ps JOIN "
            + DOC_TABLE
            + " d ON d.id = ps.contract_id AND d.deleted = false AND d.status = 'ACTIVE'"
            + " WHERE ps.deleted = false AND ps.status = 'PENDING'"
            + " AND (d.buyer_in = " + USER_PINFL + " OR d.seller_in = " + USER_PINFL + ")"
            + " UNION ALL"
            + " SELECT date_trunc('month', ps.paid_at),"
            + " COALESCE(ps.currency, 'UZS'),"
            + " CASE WHEN d.seller_in = " + USER_PINFL + " THEN 'INCOME' ELSE 'OUTCOME' END,"
            + " true, ps.total_amount"
            + " FROM "
            + PS_TABLE
            + " ps JOIN "
            + DOC_TABLE
            + " d ON d.id = ps.contract_id AND d.deleted = false"
            + " WHERE ps.deleted = false AND ps.status = 'PAID'"
            + " AND ps.paid_at IS NOT NULL"
            + " AND (d.buyer_in = " + USER_PINFL + " OR d.seller_in = " + USER_PINFL + ")"
            + ") t"
            + " WHERE month >= date_trunc('month', NOW()) - INTERVAL '5 months'"
            + " AND month < date_trunc('month', NOW()) + INTERVAL '7 months'"
            + " GROUP BY month, currency, direction, paid"
            + " ORDER BY month";

    return db.sql(sql)
        .bind("userId", userId)
        .map(
            (row, m) ->
                new BusinessStatsResponse.CashflowPoint(
                    row.get("month", String.class),
                    Currency.valueOf(row.get("currency", String.class)),
                    Direction.valueOf(row.get("direction", String.class)),
                    Boolean.TRUE.equals(row.get("paid", Boolean.class)),
                    row.get("amount", Double.class)))
        .all();
  }

  // ================ SHARED HELPERS ================

  // Tomon (buyer/seller) filtri — to'lov endi PINFL saqlaydi, user UUID shartnoma orqali.
  // idCol: PS uchun contract_id, PaidSchedule (payment_transactions) uchun document_id.
  private void appendOwnerFilter(StringBuilder sql, Direction direction, String idCol) {
    String party;
    if (direction == Direction.INCOME) {
      party = "seller_in = " + OWNER_PINFL;
    } else if (direction == Direction.OUTCOME) {
      party = "buyer_in = " + OWNER_PINFL;
    } else {
      party = "(buyer_in = " + OWNER_PINFL + " OR seller_in = " + OWNER_PINFL + ")";
    }
    sql.append(" AND ")
        .append(idCol)
        .append(" IN (SELECT id FROM document.contracts WHERE ")
        .append(party)
        .append(" AND deleted = false)");
  }

  private void appendStatusFilter(StringBuilder sql, List<PaymentScheduleStatus> statuses) {
    if (statuses != null && !statuses.isEmpty()) {
      sql.append(" AND status IN (:statuses)");
    }
  }

  private void appendDateFilter(StringBuilder sql, Instant startDate, Instant endDate) {
    if (startDate != null) sql.append(" AND contract_payment_date >= :startDate");
    if (endDate != null) sql.append(" AND contract_payment_date <= :endDate");
  }

  // To'lovlar taqvimi faqat FAOL (ACTIVE) shartnomalar to'lovlarini hisobga oladi —
  // CREATED/SIGNED_*/COMPLETED/REJECTED/CANCELLED hujjatlarniki chiqmaydi.
  private void appendActiveDocumentFilter(StringBuilder sql) {
    sql.append(" AND contract_id IN (SELECT id FROM ")
        .append(DOC_TABLE)
        .append(" WHERE status = 'ACTIVE' AND deleted = false)");
  }

  private DatabaseClient.GenericExecuteSpec bindStatusFilter(
      DatabaseClient.GenericExecuteSpec spec, List<PaymentScheduleStatus> statuses) {
    if (statuses != null && !statuses.isEmpty()) {
      spec = spec.bind("statuses", Utils.nameEnums(statuses));
    }
    return spec;
  }

  private DatabaseClient.GenericExecuteSpec bindDateFilter(
      DatabaseClient.GenericExecuteSpec spec, Instant startDate, Instant endDate) {
    if (startDate != null) spec = spec.bind("startDate", startDate);
    if (endDate != null) spec = spec.bind("endDate", endDate);
    return spec;
  }
}
