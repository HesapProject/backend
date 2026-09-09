package uz.hesap.service.common.util.message;

import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

// Topic'ga (BLOG/ARTICLE) FCM push so'rovi — notification → integration (s2s).
// integration servis haqiqiy FCM yuborishni bajaradi.
public record FirebaseTopicReply(
    UUID id, UUID dataId, String topic, TextModel title, TextModel body, NotificationType type) {}
