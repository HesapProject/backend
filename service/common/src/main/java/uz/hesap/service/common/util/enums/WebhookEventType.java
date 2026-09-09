package uz.hesap.service.common.util.enums;

// OpenAPI kaliti obuna bo'la oladigan webhook hodisalari. Kalit yaratishda
// tanlanadi (bo'sh -> hammasi). JSON payload'da `event` maydonida keladi.
public enum WebhookEventType {

  /** Shartnoma yaratildi (hali imzolanmagan). */
  CONTRACT_CREATED,

  /** Taraflardan biri imzoladi. */
  CONTRACT_SIGNED,

  /** Ikkala taraf imzoladi — shartnoma FAOL (ACTIVE). */
  CONTRACT_ACTIVE,

  /** Qarshi taraf rad etdi (REJECTED). */
  CONTRACT_REJECTED,

  /** Yaratuvchi bekor qildi (CANCELLED). */
  CONTRACT_CANCELLED,

  /** To'lov qabul qilindi (to'lov so'rovi tasdiqlandi). */
  PAYMENT_ACCEPTED
}
