package uz.hesap.service.integration.model;

import java.time.Instant;
import java.util.UUID;

public record AmoLogResponse(
    UUID id,
    String action,
    String status,
    String userId,
    String phone,
    Long amoContactId,
    String request,
    String response,
    String errorMessage,
    Instant createdAt) {}
