package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.PaymentMethod;

public record PaymentResponse(
    UUID id,
    Double amount,
    UUID userId,
    PaymentMethod paymentMethod,
    UUID createdBy,
    UUID updatedBy,
    Instant createdAt,
    Instant updatedAt) {}
