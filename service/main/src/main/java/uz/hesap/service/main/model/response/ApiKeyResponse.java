package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.common.util.enums.WebhookEventType;

/**
 * Kalit ma'lumoti — kalitning O'ZI yo'q, faqat maskalangan boshlanishi (maskedKey). Kalit matni
 * bir marta, yaratish/rotate javobida ({@link ApiKeyCreatedResponse}) beriladi.
 *
 * <p>expiresAt NULL -> abadiy; expired — muddati o'tganini frontend qayta hisoblamasligi uchun.
 */
public record ApiKeyResponse(
    UUID id,
    String name,
    String ownerIn,
    String maskedKey,
    List<ApiScope> scopes,
    Boolean allTemplates,
    List<UUID> templateIds,
    Instant expiresAt,
    Boolean expired,
    String webhookUrl,
    Boolean webhookSecretSet,
    List<WebhookEventType> webhookEvents,
    Boolean active,
    Instant revokedAt,
    Instant lastUsedAt,
    Instant createdDate) {}
