package uz.hesap.service.document.api.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.ApiKeyContext;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.document.api.openapi.model.OpenApiTemplateResponse;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.model.response.TemplateFieldResponse;
import uz.hesap.service.document.service.template.TemplateFieldService;
import uz.hesap.service.document.service.template.TemplatesService;

/**
 * Public API — shablonlar. Kalit `allTemplates=true` bo'lsa barcha nashr etilgan shablonlar,
 * aks holda faqat kalitga biriktirilgan shablonlar qaytadi.
 */
@Tag(name = "Shablonlar", description = "Kalitga ruxsat etilgan shartnoma shablonlari")
@RestController
@RequestMapping("/openapi/v1/templates")
@RequiredArgsConstructor
public class OpenApiTemplatesController {

  private final TemplatesService templatesService;
  private final TemplateFieldService templateFieldService;

  @Operation(summary = "Kalit ishlay oladigan shablonlar ro'yxati")
  @GetMapping
  public Flux<OpenApiTemplateResponse> getTemplates(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.TEMPLATES_READ);
    return templatesService
        .getAll(null, TemplateStatus.PUBLISHED)
        .filter(t -> apiKey.allowsTemplate(t.id()))
        .map(OpenApiTemplateResponse::from);
  }

  @Operation(summary = "Shablon maydonlari — shartnoma `values` massivini to'ldirish uchun")
  @GetMapping("/{templateId}/fields")
  public Flux<TemplateFieldResponse> getFields(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID templateId) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.TEMPLATES_READ);
    if (!apiKey.allowsTemplate(templateId)) {
      return Flux.error(new ForbiddenException("Bu shablon kalitga ruxsat etilmagan"));
    }
    return templateFieldService.getAll(templateId);
  }
}
