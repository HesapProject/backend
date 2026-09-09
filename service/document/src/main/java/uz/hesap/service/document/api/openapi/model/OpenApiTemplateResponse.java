package uz.hesap.service.document.api.openapi.model;

import java.util.UUID;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.model.response.TemplateResponse;

/**
 * Public API uchun shablon — ichki {@link TemplateResponse}'ning qisqartirilgan ko'rinishi. JRXML
 * manbasi (templateData*) tashqariga chiqarilmaydi.
 */
public record OpenApiTemplateResponse(
    UUID id,
    String nameUz,
    String nameRu,
    String nameEn,
    TemplateType type,
    TemplateStatus status,
    Integer witnessCount,
    String[] enabledCurrencies,
    Boolean productEnabled,
    Boolean productRequired,
    Boolean paymentScheduleEnabled,
    Boolean witnessEnabled,
    Boolean initialPaymentEnabled) {

  public static OpenApiTemplateResponse from(final TemplateResponse t) {
    return new OpenApiTemplateResponse(
        t.id(),
        t.nameUz(),
        t.nameRu(),
        t.nameEn(),
        t.templateType(),
        t.status(),
        t.witnessCount(),
        t.enabledCurrencies(),
        t.productEnabled(),
        t.productRequired(),
        t.paymentScheduleEnabled(),
        t.witnessEnabled(),
        t.initialPaymentEnabled());
  }
}
