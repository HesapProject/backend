package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.UUID;

// Admin (Control) UserInfo "Sessiyalar" tab'i uchun — sessionId + archived bilan.
public record SessionAdminResponse(
    UUID sessionId,
    String uuid,
    String os,
    String osVersion,
    String brand,
    String model,
    String device,
    String type,
    String fcmToken,
    Boolean archived,
    Instant createdDate,
    Instant lastModifiedDate) {}
