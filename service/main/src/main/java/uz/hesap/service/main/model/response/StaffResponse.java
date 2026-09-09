package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.Permission;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.domain.enums.StaffType;

// Staff javobi — company/user nomlari enrich qilinadi.
public record StaffResponse(
    UUID id,
    StaffType type,
    StaffStatus status,
    UUID companyId,
    String companyName,
    UUID userId,
    String userFullName,
    List<Permission> permissions,
    Instant createdDate,
    UUID createdBy,
    Instant lastModifiedDate,
    UUID updatedBy) {}
