package uz.hesap.service.common.util;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.CompanyType;

public record CompanyResponse(
    UUID id,
    CompanyType type,
    String name,
    String customName,
    String tin,
    Boolean isActive,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
