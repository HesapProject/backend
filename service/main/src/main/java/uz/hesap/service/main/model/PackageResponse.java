package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TemplateConfigResponse;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.enums.TariffType;

public record PackageResponse(
    UUID id,
    TextModel name,
    TextModel description,
    Double price,
    Integer stars,
    Integer duration,
    TariffType type,
    TemplateConfigResponse templateConfig,
    Integer scoringHesap,
    Integer scoringKatm,
    Integer scoringPayment,
    Instant createdDate,
    Instant lastModifiedDate) {}
