package uz.hesap.service.main.model.request;

import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.Permission;
import uz.hesap.service.main.domain.enums.StaffType;

// Staff yaratish/taklif. company — taklif qiluvchi, user — taklif qilingan; permissions —
// Permission enum ro'yxati.
public record StaffRequest(
    StaffType type, UUID companyId, UUID userId, List<Permission> permissions) {}
