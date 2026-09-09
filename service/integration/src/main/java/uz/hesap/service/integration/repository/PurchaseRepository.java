package uz.hesap.service.integration.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PurchaseEntity;

@Repository
public interface PurchaseRepository extends R2dbcRepository<PurchaseEntity, UUID> {

  Flux<PurchaseEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  // DIQQAT: property `userIn` — derived query parser undagi `In`ni IN-operatori deb o'qiydi
  // ("No property 'user'...") → startup crash. Shu sabab @Query + qo'lda LIMIT/OFFSET.
  @Query(
      "SELECT * FROM \"user\".purchases WHERE user_in = :userIn "
          + "ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PurchaseEntity> findAllByUserInOrderByCreatedAtDesc(String userIn, int limit, long offset);

  @Query("SELECT COUNT(*) FROM \"user\".purchases WHERE user_in = :userIn")
  Mono<Long> countByUserIn(String userIn);

  // Balans hisoblash: balansdan qilingan xaridlar (chiquvchi) yig'indisi.
  @Query(
      "SELECT COALESCE(SUM(amount), 0) FROM \"user\".purchases "
          + "WHERE user_in = :userIn AND payment_method = 'BALANCE'")
  Mono<Double> sumBalanceAmountByUserIn(String userIn);

  // Sana oralig'idagi chiquvchi yig'indi (xulosa uchun).
  @Query(
      "SELECT COALESCE(SUM(amount), 0) FROM \"user\".purchases WHERE user_in = :userIn "
          + "AND payment_method = 'BALANCE' AND (:from IS NULL OR created_at >= :from) "
          + "AND (:to IS NULL OR created_at <= :to)")
  Mono<Double> sumBalanceAmountByUserInBetween(String userIn, Instant from, Instant to);

  // Admin Xaridlar sahifasi: server-side sahifalash + ixtiyoriy filtrlar
  // (paket=unit_id, promo, sana oralig'i). Null filtr — o'tkazib yuboriladi.
  @Query(
      "SELECT * FROM \"user\".purchases WHERE "
          + "(:unitId IS NULL OR unit_id = CAST(:unitId AS uuid)) "
          + "AND (:promo IS NULL OR promo ILIKE :promo) "
          + "AND (:from IS NULL OR created_at >= :from) "
          + "AND (:to IS NULL OR created_at <= :to) "
          + "ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PurchaseEntity> findFiltered(
      String unitId, String promo, Instant from, Instant to, int limit, long offset);

  @Query(
      "SELECT COUNT(*) FROM \"user\".purchases WHERE "
          + "(:unitId IS NULL OR unit_id = CAST(:unitId AS uuid)) "
          + "AND (:promo IS NULL OR promo ILIKE :promo) "
          + "AND (:from IS NULL OR created_at >= :from) "
          + "AND (:to IS NULL OR created_at <= :to)")
  Mono<Long> countFiltered(String unitId, String promo, Instant from, Instant to);

  // Kompaniya tushumi (statistika): sana oralig'ida tashqaridan tushgan pul —
  // faqat real to'lov usullari (BALANCE ichki, CONTROL admin-grant — hisobga olinmaydi).
  @Query(
      "SELECT COALESCE(SUM(amount), 0) FROM \"user\".purchases "
          + "WHERE payment_method NOT IN ('BALANCE', 'CONTROL') "
          + "AND (:from IS NULL OR created_at >= :from) "
          + "AND (:to IS NULL OR created_at <= :to)")
  Mono<Double> sumRevenueBetween(Instant from, Instant to);
}
