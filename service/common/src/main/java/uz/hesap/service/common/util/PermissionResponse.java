package uz.hesap.service.common.util;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.Permission;

public record PermissionResponse(
    UUID id,
    UUID userId,
    UUID companyId,
    Permission[] permissions,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
