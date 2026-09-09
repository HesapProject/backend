package uz.hesap.service.common.util;

import java.util.UUID;

public record DocumentUsageRequest(UUID templateId, UUID userId) {}
