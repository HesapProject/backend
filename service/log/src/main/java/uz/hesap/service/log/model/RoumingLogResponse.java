package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

public record RoumingLogResponse(
    UUID id,
    UUID contractId,
    String contractNo,
    String facturaNo,
    String sellerTin,
    String buyerTin,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant requestTime,
    Instant createdDate) {}
