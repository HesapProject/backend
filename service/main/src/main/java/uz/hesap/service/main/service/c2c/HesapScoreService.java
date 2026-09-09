package uz.hesap.service.main.service.c2c;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.main.domain.HesapScoreEntity;
import uz.hesap.service.main.model.HesapScoreSignals;
import uz.hesap.service.main.model.ScoringUsageRequest;
import uz.hesap.service.main.model.UserScoringRequest;
import uz.hesap.service.main.model.response.HesapScoreResponse;
import uz.hesap.service.main.repository.HesapScoreRepository;
import uz.hesap.service.main.repository.HesapScoreSignalsRepository;
import uz.hesap.service.main.repository.UserPackageRepository;
import uz.hesap.service.main.service.PackageService;

// Hesap Score (Faza 1, ichki S1–S5) hisoblovchi. 0..1000 shkala, A–F band,
// cold-start 620, confidence=coverage, counterparty-quality q (pair-decay 0.85^(N-1)).
// Barcha og'irlik/threshold VAQTINCHALIK — prod ma'lumotda kalibrlanadi.
@Service
@Log4j2
@RequiredArgsConstructor
public class HesapScoreService {

  private final HesapScoreSignalsRepository signalsRepository;
  private final HesapScoreRepository repository;
  private final ObjectMapper objectMapper;
  // Faza 2 — paket-gated ko'rish (karta scoringidek billing).
  private final ScoringUsageService scoringUsageService;
  private final UserScoringService userScoringService;
  private final UserPackageRepository userPackageRepository;
  private final PackageService packageService;

  // Legacy sintetik sanalardan qochish uchun kesim (TODO: SystemSetting'ga ko'chirish).
  private static final Instant CUTOFF = Instant.parse("2025-01-01T00:00:00Z");
  private static final double HALF_LIFE_DAYS = 548.0; // ~18 oy
  private static final int COLD_START = 620;
  private static final int MIN_MATURED = 3; // thin-file chegarasi
  private static final Duration FRESH = Duration.ofHours(24);

  // Ichki sub-skor og'irliklari (S6/S7 tashqi Faza 1'da yo'q — jami 0.86, coverage bilan renorm).
  private static final double W1 = 0.34, W2 = 0.20, W3 = 0.16, W4 = 0.10, W5 = 0.06;
  private static final double W_TOTAL = W1 + W2 + W3 + W4 + W5;

  // GET /me — mavjud va yangi bo'lsa keshdan, aks holda sinxron hisoblab saqlaydi.
  public Mono<HesapScoreResponse> getOrCompute(String userIn) {
    return repository
        .findByUserIn(userIn)
        .flatMap(
            e ->
                isFresh(e)
                    ? Mono.just(fromEntity(e, false))
                    : computeAndStore(userIn))
        .switchIfEmpty(Mono.defer(() -> computeAndStore(userIn)));
  }

  // Faza 2 — boshqa tomon skorini paket kvotasidan yechib ko'rish (karta scoringidek).
  // O'zini ko'rish bepul. subjectIn — kimning skori, userPackageId — qaysi paketdan.
  public Mono<HesapScoreResponse> viewForRequester(String requesterIn, String subjectIn, UUID userPackageId) {
    if (subjectIn == null || subjectIn.isBlank()) {
      return Mono.error(new BadRequestException("subjectIn required"));
    }
    if (subjectIn.equals(requesterIn)) {
      return getOrCompute(subjectIn); // o'zini — bepul
    }
    return checkQuota(requesterIn, userPackageId)
        .then(getOrCompute(subjectIn))
        .flatMap(resp -> recordBilling(requesterIn, subjectIn, userPackageId).thenReturn(resp));
  }

  // Paket faol + shu foydalanuvchiniki + HESAP kvota qolganini tekshiradi.
  private Mono<Void> checkQuota(String requesterIn, UUID userPackageId) {
    if (userPackageId == null) {
      return Mono.error(new BadRequestException("PACKAGE_REQUIRED"));
    }
    return userPackageRepository
        .findById(userPackageId)
        .switchIfEmpty(Mono.error(new BadRequestException("PACKAGE_REQUIRED")))
        .flatMap(
            up -> {
              boolean invalid =
                  Boolean.TRUE.equals(up.getDeleted())
                      || !requesterIn.equals(up.getUserIn())
                      || up.getExpDate() == null
                      || up.getExpDate().isBefore(Instant.now());
              if (invalid) {
                return Mono.error(new BadRequestException("PACKAGE_REQUIRED"));
              }
              return packageService
                  .getById(up.getPackageId())
                  .flatMap(
                      pkg -> {
                        int quota = pkg.scoringHesap() == null ? 0 : pkg.scoringHesap();
                        if (quota <= 0) {
                          return Mono.error(new BadRequestException("PACKAGE_REQUIRED"));
                        }
                        return scoringUsageService
                            .usedCount(userPackageId, "HESAP")
                            .flatMap(
                                used ->
                                    used >= quota
                                        ? Mono.<Void>error(
                                            new BadRequestException("SCORING_QUOTA_EXHAUSTED"))
                                        : Mono.<Void>empty());
                      });
            });
  }

  // Kvotani sarflash (scoring_usage) + umumlashgan jurnal (user_scoring). Best-effort.
  private Mono<Void> recordBilling(String requesterIn, String subjectIn, UUID userPackageId) {
    return scoringUsageService
        .record(new ScoringUsageRequest(subjectIn, userPackageId, "HESAP", subjectIn))
        .onErrorResume(e -> Mono.empty())
        .then(
            userScoringService
                .record(new UserScoringRequest(subjectIn, "HESAP", requesterIn, subjectIn))
                .onErrorResume(e -> Mono.empty()));
  }

  // Tarixdan qayta ko'rish — PUL YECHMAYDI. Faqat so'rovchi oldin so'ragan bo'lsa
  // (user_scoring'da yozuv bor). O'zini ko'rish har doim mumkin (bepul).
  public Mono<HesapScoreResponse> reviewStored(String requesterIn, String subjectIn) {
    if (subjectIn == null || subjectIn.isBlank()) {
      return Mono.error(new BadRequestException("subjectIn required"));
    }
    if (subjectIn.equals(requesterIn)) {
      return getOrCompute(subjectIn);
    }
    return userScoringService
        .byRequesterAndUser(requesterIn, subjectIn)
        .hasElements()
        .flatMap(
            exists ->
                Boolean.TRUE.equals(exists)
                    ? getOrCompute(subjectIn)
                    : Mono.error(new ForbiddenException("no prior scoring for this subject")));
  }

  // Hisoblab, upsert qilib, javobni qaytaradi.
  public Mono<HesapScoreResponse> computeAndStore(String userIn) {
    return signalsRepository
        .getSignals(userIn, CUTOFF, HALF_LIFE_DAYS)
        .map(sig -> compute(userIn, sig))
        .flatMap(r -> persist(r).thenReturn(r.response()));
  }

  private boolean isFresh(HesapScoreEntity e) {
    return e.getUpdatedAt() != null && e.getUpdatedAt().isAfter(Instant.now().minus(FRESH));
  }

  private Mono<Void> persist(Result r) {
    return repository.upsert(
        r.userIn(), r.score(), r.band(), r.confidence(), r.coverage(),
        r.insufficient(), r.coldStart(), toJson(r.subScores()), toJson(r.inputs()), toJson(r.reasons()));
  }

  // ---- Skoring matematikasi ----

  private Result compute(String userIn, HesapScoreSignals s) {
    // Sub-skorlar (0..100), mavjud bo'lmasa null → coverage'dan tushib qoladi.
    Double s1 = null;
    if (s.nMatured() > 0) {
      double num = s.wEarly() + s.wOnTime() + 0.4 * s.wLate();
      double den = s.wEarly() + s.wOnTime() + s.wLate() + 1.5 * s.wVeryLate() + 2.0 * s.wUnpaid() + 3.0;
      s1 = 100.0 * num / den;
    }
    Double s2 = null;
    long den2 = s.completed() + s.rejected() + s.cancelled() + s.active();
    if (den2 > 0) s2 = 100.0 * s.completed() / den2;

    Double s3 = null;
    if (s.nMatured() > 0) {
      double pen =
          25.0 * Math.min(s.unpaidOverdue(), 4) / 4.0
              + 35.0 * Math.min(s.maxOverdueDays(), 90) / 90.0
              + (s.noticesAgainst() > 0 ? 20 : 0)
              + (s.claimsAgainst() > 0 ? 20 : 0);
      s3 = clamp(100 - pen, 0, 100);
    }
    Double s4 = null;
    if (s.totalContracts() > 0) {
      s4 =
          40.0 * Math.min(s.nMatured(), 10) / 10.0
              + 30.0 * Math.min(s.counterparties(), 5) / 5.0
              + 30.0 * Math.min(s.tenureMonths(), 24) / 24.0;
      s4 = clamp(s4, 0, 100);
    }
    Double s5 = null;
    if (s.totalContracts() > 0) {
      double pen = 15.0 * s.cancelsApproved() + 8.0 * Math.min(s.delays(), 5);
      s5 = clamp(100 - Math.min(100, pen), 0, 100);
    }

    // Mavjud sub-skorlar bo'yicha og'irlikli o'rtacha + coverage.
    double wsum = 0, num = 0;
    if (s1 != null) { num += W1 * s1; wsum += W1; }
    if (s2 != null) { num += W2 * s2; wsum += W2; }
    if (s3 != null) { num += W3 * s3; wsum += W3; }
    if (s4 != null) { num += W4 * s4; wsum += W4; }
    if (s5 != null) { num += W5 * s5; wsum += W5; }
    double base100 = wsum > 0 ? num / wsum : 0;
    double coverage = wsum / W_TOTAL;

    // Anti-gaming q — konsentratsiya jarima (pair-decay 0.85^(N-1)).
    double effective = 0, total = 0;
    for (Long cnt : s.pairCounts()) {
      total += cnt;
      effective += (1 - Math.pow(0.85, cnt)) / 0.15;
    }
    double q = total > 0 ? effective / total : 1.0;
    double qFactor = 0.4 + 0.6 * q;

    double scoreRaw = base100 * 10 * qFactor;
    double finalScore = coverage * scoreRaw + (1 - coverage) * COLD_START;

    // Hard gate'lar (silliq skorni bekor qiladi).
    if (s.unpaidOverdue() > 0 && s.maxOverdueDays() > 90) finalScore = Math.min(finalScore, 350);
    if (s.claimsAgainst() > 0) finalScore = Math.min(finalScore, 550);

    int score = (int) Math.round(clamp(finalScore, 0, 1000));
    boolean coldStart = s.isEmpty();
    boolean insufficient = s.nMatured() < MIN_MATURED;
    String band = insufficient ? "INSUFFICIENT_HISTORY" : bandOf(score);

    Map<String, Object> subScores = new LinkedHashMap<>();
    subScores.put("s1_punctuality", round1(s1));
    subScores.put("s2_completion", round1(s2));
    subScores.put("s3_delinquency", round1(s3));
    subScores.put("s4_volume_tenure", round1(s4));
    subScores.put("s5_friction", round1(s5));
    subScores.put("q_factor", round2(q));
    subScores.put("coverage", round2(coverage));

    Map<String, Object> inputs = new LinkedHashMap<>();
    inputs.put("nMatured", s.nMatured());
    inputs.put("wEarly", round2(s.wEarly()));
    inputs.put("wOnTime", round2(s.wOnTime()));
    inputs.put("wLate", round2(s.wLate()));
    inputs.put("wVeryLate", round2(s.wVeryLate()));
    inputs.put("wUnpaid", round2(s.wUnpaid()));
    inputs.put("unpaidOverdue", s.unpaidOverdue());
    inputs.put("maxOverdueDays", round1(s.maxOverdueDays()));
    inputs.put("completed", s.completed());
    inputs.put("rejected", s.rejected());
    inputs.put("cancelled", s.cancelled());
    inputs.put("active", s.active());
    inputs.put("counterparties", s.counterparties());
    inputs.put("tenureMonths", round1(s.tenureMonths()));
    inputs.put("totalContracts", s.totalContracts());
    inputs.put("noticesAgainst", s.noticesAgainst());
    inputs.put("claimsAgainst", s.claimsAgainst());
    inputs.put("cancelsApproved", s.cancelsApproved());
    inputs.put("delays", s.delays());

    List<String> reasons = reasonCodes(s, insufficient, q);

    HesapScoreResponse resp =
        new HesapScoreResponse(
            score, band, round2(coverage), round2(coverage), insufficient, coldStart,
            subScores, inputs, reasons, Instant.now(), false);
    return new Result(userIn, score, band, coverage, insufficient, coldStart, subScores, inputs, reasons, resp);
  }

  private List<String> reasonCodes(HesapScoreSignals s, boolean insufficient, double q) {
    List<String> r = new ArrayList<>();
    if (insufficient) r.add("INSUFFICIENT_HISTORY");
    if (s.wOnTime() + s.wEarly() > s.wLate() + s.wVeryLate() + s.wUnpaid() && s.nMatured() > 0)
      r.add("GOOD_PAYMENT_HISTORY");
    if (s.wUnpaid() > 0) r.add("UNPAID_OVERDUE");
    if (s.wLate() + s.wVeryLate() > 0) r.add("LATE_PAYMENTS");
    if (s.completed() > 0 && s.rejected() + s.cancelled() == 0) r.add("HIGH_COMPLETION");
    if (s.rejected() + s.cancelled() > 0) r.add("CONTRACT_TERMINATIONS");
    if (s.claimsAgainst() > 0) r.add("CLAIMS_RECEIVED");
    if (s.noticesAgainst() > 0) r.add("NOTICES_RECEIVED");
    if (s.totalContracts() > 0 && s.counterparties() < 2) r.add("LOW_COUNTERPARTY_DIVERSITY");
    if (q < 0.7) r.add("CONCENTRATED_COUNTERPARTIES");
    return r.size() > 5 ? r.subList(0, 5) : r;
  }

  private HesapScoreResponse fromEntity(HesapScoreEntity e, boolean stale) {
    return new HesapScoreResponse(
        e.getScore(), e.getBand(), e.getConfidence(), e.getCoverage(),
        e.getInsufficientHistory(), e.getColdStart(),
        parse(e.getSubScores()), parse(e.getInputsSnapshot()), parseList(e.getReasonCodes()),
        e.getComputedAt(), stale);
  }

  private static String bandOf(int score) {
    if (score >= 900) return "A+";
    if (score >= 800) return "A";
    if (score >= 650) return "B";
    if (score >= 500) return "C";
    if (score >= 350) return "D";
    return "F";
  }

  private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  private static Double round1(Double v) {
    return v == null ? null : Math.round(v * 10) / 10.0;
  }

  private static Double round1(double v) {
    return Math.round(v * 10) / 10.0;
  }

  private static Double round2(double v) {
    return Math.round(v * 100) / 100.0;
  }

  private String toJson(Object o) {
    try {
      return objectMapper.writeValueAsString(o);
    } catch (Exception ex) {
      log.warn("hesap-score JSON serialize failed", ex);
      return null;
    }
  }

  private Object parse(String json) {
    if (json == null) return null;
    try {
      return objectMapper.readValue(json, Object.class);
    } catch (Exception ex) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> parseList(String json) {
    if (json == null) return List.of();
    try {
      return objectMapper.readValue(json, List.class);
    } catch (Exception ex) {
      return List.of();
    }
  }

  // Ichki hisob natijasi (persist + response uchun).
  private record Result(
      String userIn,
      int score,
      String band,
      double coverage,
      boolean insufficient,
      boolean coldStart,
      Map<String, Object> subScores,
      Map<String, Object> inputs,
      List<String> reasons,
      HesapScoreResponse response) {
    Double confidence() {
      return Math.round(coverage * 100) / 100.0;
    }
  }
}
