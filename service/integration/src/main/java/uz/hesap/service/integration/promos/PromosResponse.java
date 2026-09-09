package uz.hesap.service.integration.promos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PromosResponse(
    UUID id,
    String code,
    PromosDiscountType discountType,
    Double discountAmount,
    PromosUsageType usageType,
    PromosAudience audience,
    LocalDate validFrom,
    LocalDate validTo,
    Boolean active,
    Instant createdDate,
    Instant lastModifiedDate) {}
