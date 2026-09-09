package uz.hesap.service.integration.model.plum;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ScoringStatusResponse(
    UUID id, String status, Map<String, Object> result, Instant createdAt, String type) {}
