package uz.hesap.service.integration.model.turanix;

import java.time.Instant;
import java.util.UUID;

// Tashqi (control/client) javob: Turanix MSISDN tekshirish natijasi.
public record TuranixCheckResponse(
    UUID id,
    UUID userId,
    String msisdn,
    String pinfl,
    String passSer,
    String passNum,
    String status,
    Integer resultCode,
    String description,
    String errorMessage,
    Instant createdAt) {}
