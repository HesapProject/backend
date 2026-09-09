package uz.hesap.service.integration.model;

import java.time.Instant;
import java.util.UUID;

public record PochtaLogResponse(
    UUID id,
    String action,
    String status,
    String mailId,
    String request,
    String response,
    String errorMessage,
    Instant createdAt) {}
