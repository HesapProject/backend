package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Yangi foydalanuvchi (mijoz) ro'yxatdan o'tganda (async, fire-and-forget) —
// main servis → RabbitMQ → integration. integration servis amoCRM'ga kontakt
// yaratadi (eski crm-app o'rniga). Routing key = klass nomi ("UserRegisteredEvent").
public record UserRegisteredEvent(
    UUID userId,
    String firstName,
    String lastName,
    String phone,
    String in,
    String type,
    Instant createdDate) {}
