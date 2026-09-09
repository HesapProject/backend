package uz.hesap.service.main.model.response;

/**
 * Kalit yaratilgandagi (yoki rotate qilingandagi) yagona javob — `key` faqat SHU YERDA ko'rinadi,
 * DB'da hash saqlanadi, keyin qayta ko'rsatib bo'lmaydi.
 *
 * <p>webhookSecret ham shu yerda beriladi (webhook imzosini tekshirish uchun).
 */
public record ApiKeyCreatedResponse(ApiKeyResponse apiKey, String key, String webhookSecret) {}
