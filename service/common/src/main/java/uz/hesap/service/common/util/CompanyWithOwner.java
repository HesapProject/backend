package uz.hesap.service.common.util;

import java.util.UUID;

public record CompanyWithOwner(UUID id, String inn, String name, UUID ownerId) {}
