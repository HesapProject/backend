package uz.hesap.service.integration.promos;

import java.time.LocalDate;

public record PromosRequest(
    String code,
    PromosDiscountType discountType,
    Double discountAmount,
    PromosUsageType usageType,
    PromosAudience audience,
    LocalDate validFrom,
    LocalDate validTo,
    Boolean active) {}
