package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record EskizLogResponse(
    UUID id,
    String phone,
    String content,
    Boolean isFailed,
    String error,
    Instant timestamp,
    String request,
    String response) {}
