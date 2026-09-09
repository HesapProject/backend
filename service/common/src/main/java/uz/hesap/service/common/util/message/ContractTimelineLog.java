package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

/**
 * Shartnoma timeline hodisasi — document servis RabbitMQ orqali yuboradi, log servis saqlaydi.
 *
 * <p>eventType: CREATED, SIGNED, WITNESS_SIGNED, WITNESS_REJECTED, PAYMENT_REQUEST_SENT,
 * PAYMENT_REQUEST_APPROVED, PAYMENT_REQUEST_REJECTED, DELAY_REQUEST_SENT, DELAY_REQUEST_APPROVED,
 * DELAY_REQUEST_REJECTED, PAYMENT_ACCEPTED.
 *
 * <p>role: BUYER / SELLER / WITNESS. actorIn — hodisa egasining PINFL/STIR (null bo'lishi mumkin).
 */
public record ContractTimelineLog(
    UUID contractId,
    String eventType,
    String role,
    String actorIn,
    Double amount,
    String currency,
    Instant occurredAt,
    // Amalni bajargan sessiya (user.session.id) va qurilma tavsifi — audit uchun.
    // Eski xabarlarda bo'lmasligi mumkin (null).
    UUID sessionId,
    String device) {

  // Eski (sessiyasiz) chaqiruvlar uchun qulaylik konstruktori.
  public ContractTimelineLog(
      UUID contractId,
      String eventType,
      String role,
      String actorIn,
      Double amount,
      String currency,
      Instant occurredAt) {
    this(contractId, eventType, role, actorIn, amount, currency, occurredAt, null, null);
  }
}
