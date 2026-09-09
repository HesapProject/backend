package uz.hesap.service.document.repository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.model.response.PartnerDocCount;
import uz.hesap.service.document.util.Constants;

@Repository
@RequiredArgsConstructor
public class PartnerRepository {

  private static final String DOC_TABLE = Constants.SCHEMA + "." + Constants.TABLE_DOCUMENT;

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  // userId → taraf identifikatori (COALESCE(pinfl,tin)) — cross-schema, o'zaro shartnomalar uchun.
  public Mono<String> findInById(UUID userId) {
    return r2dbcEntityTemplate
        .getDatabaseClient()
        .sql("SELECT COALESCE(pinfl, tin) AS in_val FROM \"user\".\"user\" WHERE id = :id")
        .bind("id", userId)
        .map(row -> row.get("in_val", String.class))
        .one();
  }

  // userni barcha partnerlari + document soni
  public Flux<PartnerDocCount> findPartnerCounts(UUID userId) {
    // Tomonlar self-scope uchun PINFL (buyer_in/seller_in) bilan aniqlanadi —
    // buyer_user_id/seller_user_id ko'pincha null (shartnoma ro'yxati ham _in bo'yicha
    // ishlaydi). Shu sabab partnyorlarni ham PINFL orqali topamiz: mening PINFL'imni
    // user jadvalidan olamiz, hujjatlarni buyer_in/seller_in bo'yicha scope qilamiz,
    // partnyorni esa qarama-qarshi tomon PINFL'i orqali user jadvalidan resolve qilamiz
    // (cross-schema, bitta DB).
    String sql =
        "WITH me AS (SELECT COALESCE(u.pinfl, u.tin) AS my_in"
            + " FROM \"user\".\"user\" u WHERE u.id = :userId) "
            + "SELECT partner_id, COUNT(*) AS doc_count FROM ("
            + " SELECT pu.id AS partner_id"
            + " FROM "
            + DOC_TABLE
            + " d"
            + " CROSS JOIN me"
            // Bir PINFL/STIR ko'p user qatoriga to'g'ri kelishi mumkin (dual-register,
            // dublikat/migratsiya yozuvlari) -> DISTINCT ON bilan har IN uchun ATIGA
            // BITTA user tanlanadi, aks holda bitta hamkor ro'yxatda bir necha marta chiqadi.
            + " LEFT JOIN ("
            + "   SELECT DISTINCT ON (COALESCE(pinfl, tin)) id, COALESCE(pinfl, tin) AS in_val"
            + "   FROM \"user\".\"user\""
            + "   WHERE COALESCE(pinfl, tin) IS NOT NULL AND deleted = false"
            + "   ORDER BY COALESCE(pinfl, tin), created_date"
            + " ) pu ON pu.in_val ="
            + " CASE WHEN d.buyer_in = me.my_in THEN d.seller_in ELSE d.buyer_in END"
            + " WHERE d.deleted = false AND me.my_in IS NOT NULL"
            + " AND (d.buyer_in = me.my_in OR d.seller_in = me.my_in)"
            + ") sub"
            + " WHERE partner_id IS NOT NULL"
            + " GROUP BY partner_id"
            + " ORDER BY doc_count DESC";

    return r2dbcEntityTemplate
        .getDatabaseClient()
        .sql(sql)
        .bind("userId", userId)
        .map(
            row ->
                new PartnerDocCount(
                    row.get("partner_id", UUID.class), row.get("doc_count", Long.class)))
        .all();
  }

  // userId emas, to'g'ridan-to'g'ri IN (PINFL/STIR) bo'yicha hamkorlar — taraflar
  // hujjatda _in bilan bog'langani uchun ishonchli (user_id ko'pincha null/stale).
  public Flux<PartnerDocCount> findPartnerCountsByIn(String in) {
    String sql =
        "SELECT partner_id, COUNT(*) AS doc_count FROM ("
            + " SELECT pu.id AS partner_id"
            + " FROM "
            + DOC_TABLE
            + " d"
            + " LEFT JOIN ("
            + "   SELECT DISTINCT ON (COALESCE(pinfl, tin)) id, COALESCE(pinfl, tin) AS in_val"
            + "   FROM \"user\".\"user\""
            + "   WHERE COALESCE(pinfl, tin) IS NOT NULL AND deleted = false"
            + "   ORDER BY COALESCE(pinfl, tin), created_date"
            + " ) pu ON pu.in_val ="
            + " CASE WHEN d.buyer_in = :in THEN d.seller_in ELSE d.buyer_in END"
            + " WHERE d.deleted = false"
            + " AND (d.buyer_in = :in OR d.seller_in = :in)"
            + ") sub"
            + " WHERE partner_id IS NOT NULL"
            + " GROUP BY partner_id"
            + " ORDER BY doc_count DESC";

    return r2dbcEntityTemplate
        .getDatabaseClient()
        .sql(sql)
        .bind("in", in)
        .map(
            row ->
                new PartnerDocCount(
                    row.get("partner_id", UUID.class), row.get("doc_count", Long.class)))
        .all();
  }
}
