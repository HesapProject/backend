package uz.hesap.service.common.util;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.ApiScope;

// OpenAPI kaliti bilan kelgan so'rov konteksti. JWT o'rniga `X-API-Key` header
// bo'lganda UserPrincipal ichiga yoziladi — endpointlar shu orqali kalit
// huquqlarini (scope + shablon cheklovi) tekshiradi. JWT oqimida null.
public record ApiKeyContext(
    UUID id,
    String name,
    // Kalit egasining barqaror identifikatori (PINFL/STIR).
    String ownerIn,
    List<ApiScope> scopes,
    // true — barcha shablonlar bilan ishlashi mumkin (full).
    Boolean allTemplates,
    // allTemplates=false bo'lganda ruxsat etilgan shablonlar.
    List<UUID> templateIds,
    // null — muddatsiz (abadiy).
    Instant expiresAt)
    implements Serializable {

  public boolean hasScope(final ApiScope scope) {
    return scopes != null && scopes.contains(scope);
  }

  // Shablon kalitga ruxsat etilganmi. allTemplates=true bo'lsa hammasi ochiq.
  public boolean allowsTemplate(final UUID templateId) {
    if (Boolean.TRUE.equals(allTemplates)) {
      return true;
    }
    return templateId != null && templateIds != null && templateIds.contains(templateId);
  }
}
