package uz.hesap.service.integration.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.integration.domain.enums.PurchaseType;

public record PurchaseResponse(
    UUID id,
    Double amount,
    String userIn,
    String promo,
    PaymentMethod paymentMethod,
    PurchaseType unitType,
    UUID unitId,
    UUID createdBy,
    UUID updatedBy,
    Instant createdAt,
    Instant updatedAt) {}
