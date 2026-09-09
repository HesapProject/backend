package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record OneIdLogResponse(
    UUID id,
    UUID userId,
    String firstName,
    String lastName,
    UUID companyId,
    String request,
    String response,
    String status,
    String errorMessage,
    Instant requestTime,
    Instant createdDate) {}
