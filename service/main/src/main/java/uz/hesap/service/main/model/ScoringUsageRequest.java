package uz.hesap.service.main.model;

import java.util.UUID;

// Scoring usage yozish so'rovi (integration -> main /local/scoring-usage).
public record ScoringUsageRequest(String userIn, UUID packageId, String scoringType, String scoringRef) {}
