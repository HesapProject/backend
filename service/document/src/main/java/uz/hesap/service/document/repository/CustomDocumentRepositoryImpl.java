package uz.hesap.service.document.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.Utils;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentType;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.PendingDocumentStats;
import uz.hesap.service.document.util.Constants;

@Repository
@RequiredArgsConstructor
public class CustomDocumentRepositoryImpl implements CustomDocumentRepository {

  private static final String DOC_TABLE = Constants.SCHEMA + "." + Constants.TABLE_DOCUMENT;
  private static final String TEMPLATE_TABLE = Constants.SCHEMA + "." + Constants.TABLE_TEMPLATE;
  private static final String PAID_SCHEDULE_TABLE =
      Constants.SCHEMA + "." + Constants.TABLE_PAID_SCHEDULE;

  private final DatabaseClient db;

  // Kunlik ketma-ket raqam: atomik UPSERT+RETURNING (race'siz, unikal).
  @Override
  public Mono<Integer> nextDailyNumber(LocalDate date) {
    String sql =
        "INSERT INTO document.document_seq (seq_date, last_no) VALUES (:d, 1) "
            + "ON CONFLICT (seq_date) DO UPDATE SET last_no = document.document_seq.last_no + 1 "
            + "RETURNING last_no";
    return db.sql(sql)
        .bind("d", date)
        .map((row, meta) -> row.get("last_no", Integer.class))
        .one();
  }

  // Peek: keyingi raqam (last_no + 1), increment qilmaydi. Qator yo'q bo'lsa → 1.
  @Override
  public Mono<Integer> peekDailyNumber(LocalDate date) {
    String sql =
        "SELECT last_no + 1 AS next_no FROM document.document_seq WHERE seq_date = :d";
    return db.sql(sql)
        .bind("d", date)
        .map((row, meta) -> row.get("next_no", Integer.class))
        .one()
        .defaultIfEmpty(1);
  }

  @Override
  public Mono<PendingDocumentStats> getPendingStats(String in) {
    String sql =
        """
            SELECT
              COUNT(*) FILTER (WHERE buyer_in = :in) AS as_buyer,
              COUNT(*) FILTER (WHERE seller_in = :in) AS as_seller
            FROM """
            + " "
            + DOC_TABLE
            + """
            \s
            WHERE deleted = false
              AND status = 'CREATED'
              AND (buyer_in = :in OR seller_in = :in)
            """;

    return db.sql(sql)
        .bind("in", in)
        .map(
            (row, m) ->
                new PendingDocumentStats(
                    row.get("as_buyer", Long.class), row.get("as_seller", Long.class)))
        .one()
        .defaultIfEmpty(new PendingDocumentStats(0, 0));
  }

  // Yaratishda buyer_in/seller_in ni user jadvalidan to'ldiradi (cross-schema, bir DB).
  @Override
  public Mono<Void> populatePartyIns(UUID docId, UUID buyerUserId, UUID sellerUserId) {
    String sql =
        "UPDATE "
            + DOC_TABLE
            + " SET buyer_in = (SELECT COALESCE(u.pinfl, u.tin) FROM \"user\".\"user\" u"
            + " WHERE u.id = :buyerId), seller_in = (SELECT COALESCE(u.pinfl, u.tin)"
            + " FROM \"user\".\"user\" u WHERE u.id = :sellerId) WHERE id = :docId";
    var spec = db.sql(sql).bind("docId", docId);
    spec =
        buyerUserId != null
            ? spec.bind("buyerId", buyerUserId)
            : spec.bindNull("buyerId", UUID.class);
    spec =
        sellerUserId != null
            ? spec.bind("sellerId", sellerUserId)
            : spec.bindNull("sellerId", UUID.class);
    return spec.fetch().rowsUpdated().then();
  }

  // ================ UNIVERSAL: find + count ================

  @Override
  public Flux<DocumentEntity> findFiltered(
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to,
      Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(DOC_TABLE);
    appendFilters(sql, in, statuses, templateId, search, from, to);
    sql.append(" ORDER BY created_date DESC");
    sql.append(" LIMIT :limit OFFSET :offset");

    var spec =
        bindFilters(db.sql(sql.toString()), in, statuses, templateId, search, from, to)
            .bind("limit", pageable.getPageSize())
            .bind("offset", pageable.getOffset());

    return spec.map((row, m) -> mapRow(row)).all();
  }

  @Override
  public Mono<Long> countFiltered(
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM ").append(DOC_TABLE);
    appendFilters(sql, in, statuses, templateId, search, from, to);

    var spec = bindFilters(db.sql(sql.toString()), in, statuses, templateId, search, from, to);

    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
  }

  // O'zaro shartnomalar — har ikkala taraf (inA va inB) buyer_in/seller_in da bo'lishi shart.
  private static final String BETWEEN_WHERE =
      " WHERE deleted = false"
          + " AND (buyer_in = :a OR seller_in = :a)"
          + " AND (buyer_in = :b OR seller_in = :b)"
          // Ko'rinish qoidasi (viewer = :a): yaratuvchi imzolamagan hujjat ko'rinmaydi.
          + " AND NOT (creator_in IS NOT NULL AND creator_in <> :a AND (CASE WHEN creator_in = buyer_in THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED' AND (CASE WHEN buyer_in = :a THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED')";

  @Override
  public Flux<DocumentEntity> findBetweenParties(String inA, String inB, Pageable pageable) {
    String sql =
        "SELECT * FROM "
            + DOC_TABLE
            + BETWEEN_WHERE
            + " ORDER BY created_date DESC LIMIT :limit OFFSET :offset";
    return db.sql(sql)
        .bind("a", inA)
        .bind("b", inB)
        .bind("limit", pageable.getPageSize())
        .bind("offset", pageable.getOffset())
        .map((row, m) -> mapRow(row))
        .all();
  }

  @Override
  public Mono<Long> countBetweenParties(String inA, String inB) {
    String sql = "SELECT COUNT(*) AS cnt FROM " + DOC_TABLE + BETWEEN_WHERE;
    return db.sql(sql)
        .bind("a", inA)
        .bind("b", inB)
        .map((row, m) -> row.get("cnt", Long.class))
        .one()
        .defaultIfEmpty(0L);
  }

  @Override
  public Flux<DocumentStatusCount> countByStatus(String in) {
    String sql =
        "SELECT status, COUNT(*) AS cnt FROM "
            + DOC_TABLE
            + " WHERE deleted = false AND (buyer_in = :in OR seller_in = :in)"
            // Ko'rinish qoidasi — ro'yxat (findFiltered) bilan bir xil.
            + " AND NOT (creator_in IS NOT NULL AND creator_in <> :in AND (CASE WHEN creator_in = buyer_in THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED' AND (CASE WHEN buyer_in = :in THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED')"
            + " GROUP BY status";
    return db.sql(sql)
        .bind("in", in)
        .map(
            (row, m) ->
                new DocumentStatusCount(
                    DocumentStatus.valueOf(row.get("status", String.class)),
                    row.get("cnt", Long.class)))
        .all();
  }

  @Override
  public Flux<BusinessStatsResponse.ContractCurrencyStats> getActiveStatsByCurrency(String in) {
    String sql =
        "SELECT COALESCE(currency, 'UZS') AS currency, COUNT(*) AS cnt,"
            + " COALESCE(SUM(price), 0) AS total FROM "
            + DOC_TABLE
            + " WHERE deleted = false AND status = 'ACTIVE'"
            + " AND (buyer_in = :in OR seller_in = :in)"
            + " GROUP BY COALESCE(currency, 'UZS')";
    return db.sql(sql)
        .bind("in", in)
        .map(
            (row, m) ->
                new BusinessStatsResponse.ContractCurrencyStats(
                    Currency.valueOf(row.get("currency", String.class)),
                    row.get("cnt", Long.class),
                    row.get("total", Double.class)))
        .all();
  }

  @Override
  public Flux<ContractTemplateReport> reportByTemplate(
      String in, Instant from, Instant to, DocumentStatus status) {
    // Taraf buyer_in/seller_in (PINFL/STIR) bo'yicha — in == null/bo'sh bo'lsa filtr yo'q
    // (admin hammasini ko'radi). status == null → tuzilganlar (created_date); status != null
    // → o'sha holatdagilar (last_modified_date — holatga o'tgan sana).
    String dateCol = status != null ? "d.last_modified_date" : "d.created_date";
    StringBuilder sql =
        new StringBuilder(
                "SELECT d.template_id, t.name_uz, t.name_ru, t.name_en,"
                    + " COUNT(*) AS cnt, COALESCE(SUM(d.price), 0) AS total FROM ")
            .append(DOC_TABLE)
            .append(" d LEFT JOIN ")
            .append(TEMPLATE_TABLE)
            .append(" t ON t.id = d.template_id")
            .append(" WHERE d.deleted = false")
            .append(" AND ")
            .append(dateCol)
            .append(" >= :from AND ")
            .append(dateCol)
            .append(" < :to");
    if (in != null && !in.isBlank()) {
      sql.append(" AND (d.buyer_in = :in OR d.seller_in = :in)");
    }
    if (status != null) {
      sql.append(" AND d.status = :status");
    }
    sql.append(" GROUP BY d.template_id, t.name_uz, t.name_ru, t.name_en");
    sql.append(" ORDER BY total DESC");
    var spec = db.sql(sql.toString()).bind("from", from).bind("to", to);
    if (in != null && !in.isBlank()) spec = spec.bind("in", in);
    if (status != null) spec = spec.bind("status", status.name());
    return spec
        .map(
            (row, m) ->
                new ContractTemplateReport(
                    row.get("template_id", UUID.class),
                    row.get("name_uz", String.class),
                    row.get("name_ru", String.class),
                    row.get("name_en", String.class),
                    row.get("cnt", Long.class),
                    row.get("total", Double.class) == null
                        ? 0.0
                        : row.get("total", Double.class)))
        .all();
  }

  @Override
  public Flux<ContractTemplateReport> reportPartyByTemplate(
      String sourceTable,
      String contractFkCol,
      String amountCol,
      String dateCol,
      String scopeAlias,
      String userIn,
      Instant from,
      Instant to,
      String status) {
    // Manba jadvali (x) contracts (c) bilan join — shablon va taraf identifikatori uchun.
    // scopeAlias — buyer_in/seller_in qaysi alias'da: "x" (payment-oilasi, denormalizatsiya)
    // yoki "c" (contract_product — manbada buyer_in yo'q, ota-shartnomadan). status null →
    // status filtri yo'q (masalan contract_product'da status ustuni yo'q).
    StringBuilder sql =
        new StringBuilder()
            .append("SELECT c.template_id, t.name_uz, t.name_ru, t.name_en,")
            .append(" COUNT(*) AS cnt, COALESCE(SUM(x.")
            .append(amountCol)
            .append("), 0) AS total FROM ")
            .append(Constants.SCHEMA)
            .append(".")
            .append(sourceTable)
            .append(" x JOIN ")
            .append(DOC_TABLE)
            .append(" c ON c.id = x.")
            .append(contractFkCol)
            .append(" LEFT JOIN ")
            .append(TEMPLATE_TABLE)
            .append(" t ON t.id = c.template_id")
            .append(" WHERE x.deleted = false")
            .append(" AND x.")
            .append(dateCol)
            .append(" >= :from AND x.")
            .append(dateCol)
            .append(" < :to");
    // userIn == null/bo'sh → taraf filtri yo'q (admin hammasini ko'radi).
    boolean scoped = userIn != null && !userIn.isBlank();
    if (scoped) {
      sql.append(" AND (")
          .append(scopeAlias)
          .append(".buyer_in = :userIn OR ")
          .append(scopeAlias)
          .append(".seller_in = :userIn)");
    }
    if (status != null) {
      sql.append(" AND x.status = :status");
    }
    sql.append(" GROUP BY c.template_id, t.name_uz, t.name_ru, t.name_en");
    sql.append(" ORDER BY total DESC");
    var spec = db.sql(sql.toString()).bind("from", from).bind("to", to);
    if (scoped) {
      spec = spec.bind("userIn", userIn);
    }
    if (status != null) {
      spec = spec.bind("status", status);
    }
    return spec
        .map(
            (row, m) ->
                new ContractTemplateReport(
                    row.get("template_id", UUID.class),
                    row.get("name_uz", String.class),
                    row.get("name_ru", String.class),
                    row.get("name_en", String.class),
                    row.get("cnt", Long.class),
                    row.get("total", Double.class) == null
                        ? 0.0
                        : row.get("total", Double.class)))
        .all();
  }

  @Override
  public Flux<ContractTemplateReport> reportWitnessByTemplate(
      UUID witnessId, Instant from, Instant to, String status) {
    // Guvohlik hisoboti: joriy foydalanuvchi (witness_id) guvohlikka chaqirilgan shartnomalar,
    // shablon kesimida soni. Status filtri witness_requests'da (witnesses'da status yo'q) → total = 0.
    StringBuilder sql =
        new StringBuilder()
            .append("SELECT c.template_id, t.name_uz, t.name_ru, t.name_en,")
            .append(" COUNT(*) AS cnt, CAST(0 AS double precision) AS total FROM ")
            .append(Constants.SCHEMA)
            .append(".")
            .append(Constants.TABLE_WITNESS_REQUEST)
            .append(" x JOIN ")
            .append(DOC_TABLE)
            .append(" c ON c.id = x.contract_id LEFT JOIN ")
            .append(TEMPLATE_TABLE)
            .append(" t ON t.id = c.template_id")
            .append(" WHERE x.created_date >= :from AND x.created_date < :to");
    // witnessId == null → guvoh filtri yo'q (admin hammasini ko'radi).
    if (witnessId != null) {
      sql.append(" AND x.witness_id = :witnessId");
    }
    if (status != null) {
      sql.append(" AND x.status = :status");
    }
    sql.append(" GROUP BY c.template_id, t.name_uz, t.name_ru, t.name_en");
    sql.append(" ORDER BY cnt DESC");
    var spec = db.sql(sql.toString()).bind("from", from).bind("to", to);
    if (witnessId != null) {
      spec = spec.bind("witnessId", witnessId);
    }
    if (status != null) {
      spec = spec.bind("status", status);
    }
    return spec
        .map(
            (row, m) ->
                new ContractTemplateReport(
                    row.get("template_id", UUID.class),
                    row.get("name_uz", String.class),
                    row.get("name_ru", String.class),
                    row.get("name_en", String.class),
                    row.get("cnt", Long.class),
                    row.get("total", Double.class) == null
                        ? 0.0
                        : row.get("total", Double.class)))
        .all();
  }

  // ================ SHARED HELPERS ================

  // umumiy WHERE — in (taraf identifikatori PINFL/STIR), statuses, templateId, search optional
  private void appendFilters(
      StringBuilder sql,
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to) {
    sql.append(" WHERE deleted = false");
    // Taraf identifikatori (in/STIR) — user o'chsa ham bog'lanish saqlanadi.
    if (in != null && !in.isBlank()) {
      sql.append(" AND (buyer_in = :in OR seller_in = :in)");
      // Ko'rinish qoidasi: yaratuvchi O'ZI imzolamaguncha shartnoma qarshi tomonga
      // ko'rinmaydi (yaratuvchiga va o'zi imzolab bo'lgan tomonga ko'rinadi).
      // creator_in NULL (eski hujjatlar) — qoida qo'llanmaydi.
      sql.append(
          " AND NOT (creator_in IS NOT NULL AND creator_in <> :in AND (CASE WHEN creator_in = buyer_in THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED' AND (CASE WHEN buyer_in = :in THEN buyer_status ELSE seller_status END) IS DISTINCT FROM 'ACCEPTED')");
    }
    if (statuses != null && !statuses.isEmpty()) {
      sql.append(" AND status IN (:statuses)");
    }
    if (templateId != null) {
      sql.append(" AND template_id = :templateId");
    }
    // Sana oralig'i (created_date). `to` — kun oxirigacha kiritilgan holda beriladi.
    if (from != null) {
      sql.append(" AND created_date >= :from");
    }
    if (to != null) {
      sql.append(" AND created_date <= :to");
    }
    if (search != null && !search.isBlank()) {
      // Raqam BO'YICHA yoki taraf (buyer/seller) ismi bo'yicha — jismoniy shaxsda
      // F.I.SH (first_name + last_name), yuridikda legal_name. buyer_in/seller_in
      // PINFL yoki STIR bo'lgani uchun ikkalasiga ham (pinfl, tin) solishtiramiz.
      // Cross-schema (bitta Postgres DB, "user"."user" — populatePartyIns'dagidek).
      // MUHIM: bu IN(...) shaklida — buyer_in/seller_in'ga CORRELATED EXISTS emas —
      // aks holda Postgres subquery'ni har bir contracts qatori uchun qayta bajaradi
      // (46k+ userli jadvalda bu timeout'ga olib keldi, prodda tekshirilgan: EXPLAIN
      // cost correlated holda ~11.6M, IN(...) holda ~13k — chunki IN(...) subquery
      // bitta marta hash sifatida quriladi).
      String matchingIdentsSql =
          "SELECT pinfl FROM \"user\".\"user\" WHERE pinfl IS NOT NULL AND"
              + " (TRIM(COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')) ILIKE :search"
              + "  OR legal_name ILIKE :search)"
              + " UNION"
              + " SELECT tin FROM \"user\".\"user\" WHERE tin IS NOT NULL AND"
              + " (TRIM(COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')) ILIKE :search"
              + "  OR legal_name ILIKE :search)";
      sql.append(
          " AND (number ILIKE :search"
              + " OR buyer_in IN ("
              + matchingIdentsSql
              + ")"
              + " OR seller_in IN ("
              + matchingIdentsSql
              + "))");
    }
  }

  // umumiy parametr bind
  private DatabaseClient.GenericExecuteSpec bindFilters(
      DatabaseClient.GenericExecuteSpec spec,
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to) {
    if (in != null && !in.isBlank()) spec = spec.bind("in", in);
    if (statuses != null && !statuses.isEmpty()) {
      spec = spec.bind("statuses", Utils.nameEnums(statuses));
    }
    if (templateId != null) spec = spec.bind("templateId", templateId);
    if (search != null && !search.isBlank()) {
      spec = spec.bind("search", "%" + search.trim() + "%");
    }
    if (from != null) spec = spec.bind("from", from);
    if (to != null) spec = spec.bind("to", to);
    return spec;
  }

  // ================ ROW MAPPING ================

  private DocumentEntity mapRow(io.r2dbc.spi.Row row) {
    DocumentEntity e = new DocumentEntity();
    e.setId(row.get("id", UUID.class));
    e.setBuyerIn(row.get("buyer_in", String.class));
    e.setSellerIn(row.get("seller_in", String.class));
    e.setTemplateId(row.get("template_id", UUID.class));
    e.setNumber(row.get("number", String.class));
    String statusStr = row.get("status", String.class);
    if (statusStr != null) e.setStatus(DocumentStatus.valueOf(statusStr));
    String type = row.get("type", String.class);
    if (type != null) e.setType(DocumentType.valueOf(type));
    e.setPrice(row.get("price", Double.class));
    String currencyStr = row.get("currency", String.class);
    if (currencyStr != null) e.setCurrency(Currency.valueOf(currencyStr));
    e.setInitialPayment(row.get("initial_payment", Double.class));
    e.setDeliveryAt(row.get("delivery_at", Instant.class));
    e.setDocumentJson(row.get("document_json", String.class));
    e.setDeleted(row.get("deleted", Boolean.class));
    String buyerSt = row.get("buyer_status", String.class);
    if (buyerSt != null) e.setBuyerStatus(DocumentPartyStatus.valueOf(buyerSt));
    String sellerSt = row.get("seller_status", String.class);
    if (sellerSt != null) e.setSellerStatus(DocumentPartyStatus.valueOf(sellerSt));
    e.setCreatorIn(row.get("creator_in", String.class));
    e.setCreatedDate(row.get("created_date", Instant.class));
    e.setLastModifiedDate(row.get("last_modified_date", Instant.class));
    e.setVersion(row.get("version", Long.class));
    return e;
  }
}
