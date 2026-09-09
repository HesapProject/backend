package uz.hesap.service.main.repository;

import static org.springframework.data.relational.core.query.Criteria.where;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.response.AgeBucketCount;

@Repository
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

  private final R2dbcEntityTemplate template;
  private final DatabaseClient databaseClient;

  // SELECT u.* + cross-schema agregatlar (shartnoma soni / SUMMA balans /
  // oxirgi tashrif epoch-ms). Hammasi bitta `hesap` DB'da — LATERAL bilan bir
  // marta hisoblanadi, ham filtr (WHERE), ham ustun uchun ishlatiladi.
  // DIQQAT: balans `billing.balance`da — `"user".balance` teardown'da DROP
  // qilingan (20260618-60), billing→user migratsiyasi qisman qolgan.
  private static final String AGG_FROM =
      """
      FROM "user"."user" u
      %s
      LEFT JOIN LATERAL (
        SELECT COUNT(*) AS cnt FROM document.contracts d
        WHERE d.deleted = false
          AND (d.buyer_in = COALESCE(u.pinfl, u.tin)
            OR d.seller_in = COALESCE(u.pinfl, u.tin))
      ) cc ON true
      LEFT JOIN LATERAL (
        SELECT b.balance FROM billing.balance b
        WHERE b.unique_id = u.id AND b.balance_type = 'SUMMA' AND b.deleted = false
        ORDER BY b.last_modified_date DESC LIMIT 1
      ) bal ON true
      LEFT JOIN LATERAL (
        SELECT (EXTRACT(EPOCH FROM MAX(s.timestamp)) * 1000)::bigint AS last_visit_ms
        FROM "user"."session" s WHERE s.user_id = u.id
      ) lv ON true
      """;

  // Xodimlik aloqasi — eski user_company teardown'da o'chirilgan, o'rniga staff.
  private static final String COMPANY_JOIN =
      " INNER JOIN \"user\".staff uc ON u.id = uc.user_id AND uc.deleted = false"
          + " AND uc.status = 'ACCEPTED' ";

  @Override
  public Flux<AdminUserRow> findByFilter(AdminUserFilter f, Pageable pageable) {
    var tokens = searchTokens(f.search());
    String from = String.format(AGG_FROM, f.companyId() != null ? COMPANY_JOIN : "");
    String sql =
        "SELECT u.*, cc.cnt AS contracts_count, bal.balance AS summa_balance,"
            + " lv.last_visit_ms AS last_visit_ms "
            + from
            + buildWhere(f, tokens)
            + " ORDER BY u.created_date DESC LIMIT :limit OFFSET :offset";

    var spec = bindFilter(databaseClient.sql(sql), f, tokens);
    return spec.bind("limit", pageable.getPageSize())
        .bind("offset", pageable.getOffset())
        .map(
            (row, meta) ->
                new AdminUserRow(
                    template.getConverter().read(UserEntity.class, row, meta),
                    toInt(row.get("contracts_count", Long.class)),
                    row.get("summa_balance", Double.class),
                    toInstant(row.get("last_visit_ms", Long.class))))
        .all();
  }

  @Override
  public Mono<Long> countByFilter(AdminUserFilter f) {
    var tokens = searchTokens(f.search());
    String from = String.format(AGG_FROM, f.companyId() != null ? COMPANY_JOIN : "");
    String sql = "SELECT COUNT(*) " + from + buildWhere(f, tokens);
    return bindFilter(databaseClient.sql(sql), f, tokens)
        .map(row -> row.get(0, Long.class))
        .one();
  }

  // Mijozlar (CLIENT) yosh taqsimoti — yosh PINFL'dan (JShShIR):
  // 1-belgi asr+jins (1,2->18xx; 3,4->19xx; 5,6->20xx), 2-7 DDMMYY.
  // to_date lenient (xato sanada exception bermaydi). Oraliqlar yarim-ochiq.
  private static final String AGE_SQL_HEAD =
      """
      SELECT bucket, COUNT(*) AS cnt FROM (
        SELECT CASE
          WHEN age_years < 16 THEN '0-16'
          WHEN age_years < 18 THEN '16-18'
          WHEN age_years < 25 THEN '18-25'
          WHEN age_years < 30 THEN '25-30'
          WHEN age_years < 35 THEN '30-35'
          WHEN age_years < 45 THEN '35-45'
          WHEN age_years < 60 THEN '45-60'
          ELSE '60+'
        END AS bucket
        FROM (
          SELECT date_part('year', age(to_date(
            (CASE WHEN left(pinfl,1) IN ('1','2') THEN '18'
                  WHEN left(pinfl,1) IN ('3','4') THEN '19'
                  ELSE '20' END)
            || substring(pinfl,6,2) || substring(pinfl,4,2) || substring(pinfl,2,2),
            'YYYYMMDD')))::int AS age_years
          FROM "user"."user"
          WHERE type = 'CLIENT' AND deleted = false
            AND pinfl ~ '^[1-6][0-9]{13}$'
      """;

  private static final String AGE_SQL_TAIL =
      """
        ) a
      ) b
      GROUP BY bucket
      """;

  @Override
  public Flux<AgeBucketCount> clientAgeDistribution(Instant createdFrom, Instant createdTo) {
    String sql = AGE_SQL_HEAD + dateFilterSql(createdFrom, createdTo) + AGE_SQL_TAIL;
    return bindDateFilter(databaseClient.sql(sql), createdFrom, createdTo)
        .map(row -> new AgeBucketCount(row.get("bucket", String.class), row.get("cnt", Long.class)))
        .all();
  }

  // Mijozlar jinsi — PINFL 1-belgisi toq (1,3,5) -> MALE, juft (2,4,6) -> FEMALE.
  private static final String GENDER_SQL_HEAD =
      """
      SELECT CASE WHEN left(pinfl,1)::int % 2 = 1 THEN 'MALE' ELSE 'FEMALE' END AS bucket,
             COUNT(*) AS cnt
      FROM "user"."user"
      WHERE type = 'CLIENT' AND deleted = false
        AND pinfl ~ '^[1-6][0-9]{13}$'
      """;

  private static final String GENDER_SQL_TAIL = " GROUP BY 1";

  @Override
  public Flux<AgeBucketCount> clientGenderDistribution(Instant createdFrom, Instant createdTo) {
    String sql = GENDER_SQL_HEAD + dateFilterSql(createdFrom, createdTo) + GENDER_SQL_TAIL;
    return bindDateFilter(databaseClient.sql(sql), createdFrom, createdTo)
        .map(row -> new AgeBucketCount(row.get("bucket", String.class), row.get("cnt", Long.class)))
        .all();
  }

  // created_date sana filtri (ikkalasi ham ixtiyoriy) — SQL bo'lagi + bind.
  private static String dateFilterSql(Instant createdFrom, Instant createdTo) {
    String s = "";
    if (createdFrom != null) s += " AND created_date >= :createdFrom";
    if (createdTo != null) s += " AND created_date <= :createdTo";
    return s;
  }

  private DatabaseClient.GenericExecuteSpec bindDateFilter(
      DatabaseClient.GenericExecuteSpec spec, Instant createdFrom, Instant createdTo) {
    if (createdFrom != null) spec = spec.bind("createdFrom", createdFrom);
    if (createdTo != null) spec = spec.bind("createdTo", createdTo);
    return spec;
  }

  // WHERE — barcha optional shartlar (mavjud deleted/type/search + yangi
  // isVerified/shartnoma/balans/oxirgi tashrif oraliqlari).
  private String buildWhere(AdminUserFilter f, java.util.List<String> tokens) {
    StringBuilder sb = new StringBuilder(" WHERE 1=1");
    if (!Boolean.TRUE.equals(f.deleted())) sb.append(" AND u.deleted = false");
    if (f.companyId() != null) sb.append(" AND uc.company_id = :companyId");
    if (f.type() != null) sb.append(" AND u.type = :type");
    if (f.isVerified() != null) sb.append(" AND u.is_verified = :isVerified");
    for (int i = 0; i < tokens.size(); i++) {
      String p = ":s" + i;
      sb.append(" AND (u.email ILIKE ").append(p);
      sb.append(" OR u.phone ILIKE ").append(p);
      sb.append(" OR u.first_name ILIKE ").append(p);
      sb.append(" OR u.last_name ILIKE ").append(p);
      sb.append(" OR u.legal_name ILIKE ").append(p);
      sb.append(" OR u.pinfl ILIKE ").append(p).append(")");
    }
    if (f.contractCountFrom() != null) sb.append(" AND cc.cnt >= :contractFrom");
    if (f.contractCountTo() != null) sb.append(" AND cc.cnt <= :contractTo");
    if (f.balanceFrom() != null) sb.append(" AND bal.balance >= :balanceFrom");
    if (f.balanceTo() != null) sb.append(" AND bal.balance <= :balanceTo");
    if (f.lastVisitFrom() != null) sb.append(" AND lv.last_visit_ms >= :lastFromMs");
    if (f.lastVisitTo() != null) sb.append(" AND lv.last_visit_ms <= :lastToMs");
    // Ro'yxatdan o'tish sanasi oralig'i (statistika davri uchun).
    if (f.createdFrom() != null) sb.append(" AND u.created_date >= :createdFrom");
    if (f.createdTo() != null) sb.append(" AND u.created_date <= :createdTo");
    // Statistika: faqat haqiqiy PINFL (JShShIR)ga ega mijozlar. Xuddi yosh/jins
    // taqsimotidagi kabi regex — PINFL'siz yoki noto'g'ri formatdagilar sanalmaydi.
    if (Boolean.TRUE.equals(f.hasPinfl())) sb.append(" AND u.pinfl ~ '^[1-6][0-9]{13}$'");
    return sb.toString();
  }

  private DatabaseClient.GenericExecuteSpec bindFilter(
      DatabaseClient.GenericExecuteSpec spec, AdminUserFilter f, java.util.List<String> tokens) {
    if (f.companyId() != null) spec = spec.bind("companyId", f.companyId());
    if (f.type() != null) spec = spec.bind("type", f.type().name());
    if (f.isVerified() != null) spec = spec.bind("isVerified", f.isVerified());
    for (int i = 0; i < tokens.size(); i++) spec = spec.bind("s" + i, tokens.get(i));
    if (f.contractCountFrom() != null) spec = spec.bind("contractFrom", f.contractCountFrom());
    if (f.contractCountTo() != null) spec = spec.bind("contractTo", f.contractCountTo());
    if (f.balanceFrom() != null) spec = spec.bind("balanceFrom", f.balanceFrom());
    if (f.balanceTo() != null) spec = spec.bind("balanceTo", f.balanceTo());
    if (f.lastVisitFrom() != null) spec = spec.bind("lastFromMs", f.lastVisitFrom().toEpochMilli());
    if (f.lastVisitTo() != null) spec = spec.bind("lastToMs", f.lastVisitTo().toEpochMilli());
    if (f.createdFrom() != null) spec = spec.bind("createdFrom", f.createdFrom());
    if (f.createdTo() != null) spec = spec.bind("createdTo", f.createdTo());
    return spec;
  }

  // Qidiruvni so'zlarga ajratadi — har bir so'z alohida (AND), barcha
  // maydonlardan birortasiga (OR) mos kelishi kerak. "%token%" ko'rinishida.
  private java.util.List<String> searchTokens(String search) {
    java.util.List<String> tokens = new java.util.ArrayList<>();
    if (search != null && !search.isBlank()) {
      for (String t : search.trim().split("\\s+")) {
        if (!t.isBlank()) tokens.add("%" + t + "%");
      }
    }
    return tokens;
  }

  private static Integer toInt(Long v) {
    return v == null ? null : v.intValue();
  }

  private static java.time.Instant toInstant(Long epochMs) {
    return epochMs == null ? null : java.time.Instant.ofEpochMilli(epochMs);
  }

  @Override
  public Flux<UserEntity> findEmployees(String search, Role role, Pageable pageable) {
    return template
        .select(UserEntity.class)
        .matching(buildUserCriteria(Boolean.FALSE, search, UserType.ADMIN, role).with(pageable))
        .all();
  }

  @Override
  public Mono<Long> countEmployees(String search, Role role) {
    return template.count(
        buildUserCriteria(Boolean.FALSE, search, UserType.ADMIN, role), UserEntity.class);
  }

  @Override
  public Flux<UserEntity> searchClients(String searchTerm) {
    Criteria criteria =
        Criteria.where("deleted").is(Boolean.FALSE).and("type").is(UserType.CLIENT.name());

    if (searchTerm != null && !searchTerm.isBlank()) {
      // Bo'sh joy bilan ajratilgan har bir so'z alohida (AND) — "ism sharif"
      // (masalan "Qodirov Javoxir Akmal o'g'li") ham topilsin. Har bir so'z
      // quyidagi maydonlardan (OR) biriga mos kelishi kerak:
      // IN/PINFL, STIR/TIN, telefon, ism, familiya, sharif, username.
      for (String token : searchTerm.trim().split("\\s+")) {
        if (token.isBlank()) continue;
        String likeTerm = "%" + token + "%";
        criteria =
            criteria.and(
                where("pinfl")
                    .like(likeTerm)
                    .ignoreCase(true)
                    .or(where("tin").like(likeTerm).ignoreCase(true))
                    .or(where("phone").like(likeTerm).ignoreCase(true))
                    .or(where("firstName").like(likeTerm).ignoreCase(true))
                    .or(where("lastName").like(likeTerm).ignoreCase(true))
                    .or(where("midName").like(likeTerm).ignoreCase(true))
                    .or(where("username").like(likeTerm).ignoreCase(true)));
      }
    }

    return template.select(UserEntity.class).matching(Query.query(criteria)).all();
  }

  private Query buildUserCriteria(Boolean deleted, String searchTerm, UserType type, Role role) {
    Criteria criteria = Criteria.empty();
    if (!Boolean.TRUE.equals(deleted)) {
      criteria = criteria.and(where("deleted").is(Boolean.FALSE));
    }
    if (type != null) criteria = criteria.and(where("type").is(type.name()));
    if (role != null) criteria = criteria.and(where("role").is(role.name()));

    if (searchTerm != null && !searchTerm.isBlank()) {
      // Bo'sh joy bilan ajratilgan har bir so'z alohida (AND) qidiriladi:
      // "Javoxir Qodirov" → "Javoxir" (firstName) AND "Qodirov" (lastName).
      // Har bir so'z barcha maydonlardan birortasiga (OR) mos kelishi kerak.
      for (String token : searchTerm.trim().split("\\s+")) {
        if (token.isBlank()) continue;
        criteria = criteria.and(searchTokenCriteria("%" + token + "%"));
      }
    }
    return Query.query(criteria);
  }

  // Bitta so'z bo'yicha barcha qidiruv maydonlari (OR), katta/kichik harf farqsiz.
  private Criteria searchTokenCriteria(String likeTerm) {
    return where("email")
        .like(likeTerm)
        .ignoreCase(true)
        .or(where("phone").like(likeTerm).ignoreCase(true))
        .or(where("firstName").like(likeTerm).ignoreCase(true))
        .or(where("lastName").like(likeTerm).ignoreCase(true))
        // Tashkilot nomi (yuridik shaxs) va PINFL/IN bo'yicha ham qidiruv.
        .or(where("legalName").like(likeTerm).ignoreCase(true))
        .or(where("in").like(likeTerm).ignoreCase(true));
  }

}
