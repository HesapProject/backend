package uz.hesap.service.common.util;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.TariffType;

public record TariffResponse(
    UUID id,
    TextModel name,
    TextModel description,
    Integer stars,
    Double price,
    Integer duration,
    TariffType type,
    TemplateConfigResponse templateConfig,
    Instant createdDate,
    Instant lastModifiedDate) {}
