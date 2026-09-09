package uz.hesap.service.common.util;

import java.util.UUID;
import uz.hesap.service.common.util.enums.Role;

public record UserCompanyRequest(UUID userId, Role role, UUID companyId) {}
