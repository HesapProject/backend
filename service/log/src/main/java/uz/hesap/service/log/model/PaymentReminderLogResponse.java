package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.UUID;

// PaymentCronService cron run tarixi (Monitoring UI'da ko'rsatiladi).
public record PaymentReminderLogResponse(
    UUID id,
    Instant startedAt,
    Instant completedAt,
    Integer totalCandidates,
    Integer sentSuccess,
    Integer sentFailed,
    String errorMessage) {}
