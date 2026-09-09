package uz.hesap.service.integration.model;

import uz.hesap.service.common.util.message.NotificationEvent;

public record TemplateNotificationRequest(
    NotificationEvent event,
    Boolean smsEnabled,
    String smsText,
    Boolean firebaseEnabled,
    String firebaseText) {}
