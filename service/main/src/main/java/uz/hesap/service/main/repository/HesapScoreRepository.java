package uz.hesap.service.main.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.HesapScoreEntity;

@Repository
public interface HesapScoreRepository extends R2dbcRepository<HesapScoreEntity, java.util.UUID> {

  // DIQQAT: `user_in` bilan tugagani uchun derived `findByUserIn` Spring Data'da IN-keyword
  // deb o'qiladi (main crashloop sababi bo'lgan) — shuning uchun @Query.
  @Query("SELECT * FROM \"user\".hesap_score WHERE user_in = :userIn")
  Mono<HesapScoreEntity> findByUserIn(@Param("userIn") String userIn);

  // user_in bo'yicha upsert (yagona qator). Vaqt ustunlari SQL'da NOW() bilan.
  @Modifying
  @Query(
      """
      INSERT INTO "user".hesap_score
        (user_in, score, band, confidence, coverage, insufficient_history, cold_start,
         sub_scores, inputs_snapshot, reason_codes, computed_at, updated_at)
      VALUES
        (:userIn, :score, :band, :confidence, :coverage, :insufficientHistory, :coldStart,
         :subScores, :inputsSnapshot, :reasonCodes, NOW(), NOW())
      ON CONFLICT (user_in) DO UPDATE SET
         score = :score, band = :band, confidence = :confidence, coverage = :coverage,
         insufficient_history = :insufficientHistory, cold_start = :coldStart,
         sub_scores = :subScores, inputs_snapshot = :inputsSnapshot, reason_codes = :reasonCodes,
         updated_at = NOW()
      """)
  Mono<Void> upsert(
      @Param("userIn") String userIn,
      @Param("score") Integer score,
      @Param("band") String band,
      @Param("confidence") Double confidence,
      @Param("coverage") Double coverage,
      @Param("insufficientHistory") Boolean insufficientHistory,
      @Param("coldStart") Boolean coldStart,
      @Param("subScores") String subScores,
      @Param("inputsSnapshot") String inputsSnapshot,
      @Param("reasonCodes") String reasonCodes);
}
