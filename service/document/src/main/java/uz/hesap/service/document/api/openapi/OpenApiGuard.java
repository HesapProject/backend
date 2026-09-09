package uz.hesap.service.document.api.openapi;

import java.util.UUID;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.ApiKeyContext;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.ApiScope;

/**
 * Public API tekshiruvlari — kalit konteksti, huquqlar (scope) va shablon cheklovi.
 *
 * <p>Kalit `allTemplates=true` bo'lsa har qanday shablon bilan ishlaydi (full); aks holda faqat
 * kalit yaratishda tanlangan shablonlar.
 */
public final class OpenApiGuard {

  private OpenApiGuard() {}

  // /openapi/** yo'llariga faqat X-API-Key bilan kelinadi — konteksti yo'q bo'lsa rad etamiz.
  public static ApiKeyContext context(final UserPrincipal principal) {
    ApiKeyContext apiKey = principal != null ? principal.apiKey() : null;
    if (apiKey == null) {
      throw new ForbiddenException("Bu endpoint faqat X-API-Key bilan ishlaydi");
    }
    return apiKey;
  }

  public static ApiKeyContext requireScope(final UserPrincipal principal, final ApiScope scope) {
    ApiKeyContext apiKey = context(principal);
    if (!apiKey.hasScope(scope)) {
      throw new ForbiddenException("Kalitda '" + scope.name() + "' huquqi yo'q");
    }
    return apiKey;
  }

  public static void requireTemplate(final ApiKeyContext apiKey, final UUID templateId) {
    if (templateId == null) {
      throw new ForbiddenException("templateId ko'rsatilishi shart");
    }
    if (!apiKey.allowsTemplate(templateId)) {
      throw new ForbiddenException("Bu shablon kalitga ruxsat etilmagan");
    }
  }

  // Kalit egasining barqaror identifikatori — shartnoma yaratuvchisi/filtri shu bo'yicha.
  public static String ownerIn(final UserPrincipal principal) {
    ApiKeyContext apiKey = context(principal);
    if (apiKey.ownerIn() != null && !apiKey.ownerIn().isBlank()) {
      return apiKey.ownerIn();
    }
    String in = principal.user() != null ? principal.user().identifier() : null;
    if (in == null || in.isBlank()) {
      throw new ForbiddenException("Kalit egasining identifikatori aniqlanmadi");
    }
    return in;
  }

  // Shartnoma kalit egasiga tegishlimi (taraf yoki yaratuvchi).
  public static boolean belongsTo(
      final String ownerIn, final String buyerIn, final String sellerIn, final String creatorIn) {
    return ownerIn.equals(buyerIn) || ownerIn.equals(sellerIn) || ownerIn.equals(creatorIn);
  }
}
