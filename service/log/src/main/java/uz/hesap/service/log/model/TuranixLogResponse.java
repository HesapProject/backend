package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record TuranixLogResponse(
    UUID id,
    UUID userId,
    String msisdn,
    String pinfl,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant requestTime,
    Instant createdDate) {}
