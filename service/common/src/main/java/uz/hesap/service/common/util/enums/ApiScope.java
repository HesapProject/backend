package uz.hesap.service.common.util.enums;

// OpenAPI kaliti huquqlari. Kalit yaratishda tanlanadi.
public enum ApiScope {

  /** Shablonlar ro'yxatini va maydonlarini o'qish. */
  TEMPLATES_READ,

  /** Shartnomalarni o'qish (ro'yxat, bitta, PDF). */
  CONTRACTS_READ,

  /** Shartnoma yaratish/tahrirlash. */
  CONTRACTS_WRITE,

  /** Shartnoma tomonlarini (mijozni) PINFL/STIR bo'yicha tekshirish. */
  CLIENTS_READ
}
