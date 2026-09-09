package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.WebhookEventType;

/**
 * OpenAPI webhook hodisasi — document servis RabbitMQ orqali yuboradi, integration servis
 * hodisaga obuna bo'lgan API kalitlarining `webhookUrl`iga HTTP POST qilib yetkazadi.
 *
 * <p>Qabul qiluvchilar shartnoma taraflari (buyerIn/sellerIn) va yaratuvchisi (creatorIn) —
 * integration servis shu identifikatorlar bo'yicha aktiv kalitlarni topadi.
 *
 * <p>actorIn — hodisani sodir qilgan taraf (yaratish/imzo/rad), null bo'lishi mumkin.
 */
public record WebhookEvent(
    WebhookEventType event,
    UUID contractId,
    String contractNumber,
    String status,
    UUID templateId,
    String buyerIn,
    String sellerIn,
    String creatorIn,
    String actorIn,
    Double amount,
    String currency,
    Instant occurredAt) {}
