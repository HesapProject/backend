package uz.hesap.service.integration.model.katm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Tashqi (control/client) javob: KATM kredit tarixi so'rovi/hisoboti.
// pToken qaytarilmaydi (ichki). reportBase64 — XML kredit tarixi (Base64),
// status COMPLETED bo'lganda to'ladi.
public record CreditHistoryResponse(
    UUID id,
    UUID userId,
    String pinfl,
    String pClientId,
    String pClaimId,
    String language,
    String status,
    BigDecimal amount,
    Boolean isFree,
    String resultMessage,
    String reportBase64,
    Instant createdAt,
    Instant completedAt) {}
