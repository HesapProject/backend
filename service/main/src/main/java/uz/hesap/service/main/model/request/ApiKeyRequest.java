package uz.hesap.service.main.model.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.common.util.enums.WebhookEventType;

/**
 * OpenAPI kalitini yaratish/tahrirlash so'rovi.
 *
 * <p>expiresAt — kalit amal qilish muddati; NULL bo'lsa kalit abadiy (muddatsiz).
 *
 * <p>allTemplates=true — kalit BARCHA shablonlar bilan ishlay oladi (full). false bo'lsa faqat
 * templateIds ro'yxatidagi shablonlar bo'yicha shartnoma tuzish/o'qish mumkin.
 *
 * <p>ownerIn — faqat admin (Control) yaratganda ishlatiladi: kalit qaysi mijoz/kompaniya nomidan
 * berilishi. Kabinetdan kelganda e'tiborga olinmaydi (o'z identifikatori qo'yiladi).
 */
public record ApiKeyRequest(
    String name,
    String ownerIn,
    List<ApiScope> scopes,
    Boolean allTemplates,
    List<UUID> templateIds,
    Instant expiresAt,
    String webhookUrl,
    List<WebhookEventType> webhookEvents) {}
