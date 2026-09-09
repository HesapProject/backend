package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record MyIdLogResponse(
    UUID id,
    UUID userId,
    UUID companyId,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant requestTime,
    Instant createdDate) {}
