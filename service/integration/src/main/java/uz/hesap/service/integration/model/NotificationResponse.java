package uz.hesap.service.integration.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.message.NotificationType;

@Builder
public record NotificationResponse(
    UUID id,
    UUID dataId,
    NotificationType type,
    TextModel title,
    TextModel body,
    String image,
    Boolean isViewed,
    Instant createdAt) {}
