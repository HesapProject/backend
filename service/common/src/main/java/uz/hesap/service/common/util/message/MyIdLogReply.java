package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

public record MyIdLogReply(
    UUID companyId,
    UUID userId,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
