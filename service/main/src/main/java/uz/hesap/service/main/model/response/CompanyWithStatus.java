package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.CompanyStatus;
import uz.hesap.service.common.util.enums.CompanyType;
import uz.hesap.service.common.util.enums.Role;

public record CompanyWithStatus(
    UUID id,
    CompanyType type,
    Role role,
    CompanyStatus status,
    String name,
    String customName,
    String tin,
    Boolean isActive,
    Boolean isMain,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
