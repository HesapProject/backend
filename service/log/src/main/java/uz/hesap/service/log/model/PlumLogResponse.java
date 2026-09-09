package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record PlumLogResponse(
    UUID id,
    UUID userId,
    UUID cardId,
    String type,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant requestTime,
    Instant createdDate) {}
