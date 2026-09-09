package uz.hesap.service.integration.model.plum;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import uz.hesap.service.integration.domain.enums.CardType;
import uz.hesap.service.integration.domain.enums.ScoringStatus;

// Control mijoz info "Skoring" tab'i uchun — tarix jadvali.
public record ScoringResponse(
    UUID id,
    UUID cardId,
    CardType type,
    ScoringStatus status,
    Instant createdAt,
    Map<String, Object> response,
    Map<String, Object> uzcard,
    Map<String, Object> humo) {}
