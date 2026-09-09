package uz.hesap.service.main.model.request;

import java.util.UUID;
import uz.hesap.service.common.util.enums.Permission;

public record PermissionRequest(UUID userId, Permission[] permissions) {}
