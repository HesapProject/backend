package uz.hesap.service.common.util;

import java.util.UUID;

public record CompanyBasicResponse(UUID id, String name, String customName, String tin) {}
