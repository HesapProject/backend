package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.List;

// GET /main/v1/hesap-score/me javobi. subScores/inputsSnapshot — tushuntiruvchanlik uchun.
// insufficientHistory=true bo'lsa mijozga xom raqam ko'rsatilmaydi (band = INSUFFICIENT_HISTORY).
public record HesapScoreResponse(
    Integer score,
    String band,
    Double confidence,
    Double coverage,
    Boolean insufficientHistory,
    Boolean coldStart,
    Object subScores,
    Object inputsSnapshot,
    List<String> reasonCodes,
    Instant computedAt,
    Boolean stale) {}
