package uz.hesap.service.common.util;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken,
    Instant createdDate,
    Instant lastModifiedDate) {}
