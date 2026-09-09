package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.ExchangeMode;
import uz.hesap.service.document.domain.enums.RoumingType;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.domain.enums.VerificationType;

public record TemplateResponse(
    UUID id,
    UUID templateId,
    UUID companyId,
    String nameUz,
    String nameRu,
    String nameEn,
    String sellerNameUz,
    String sellerNameRu,
    String sellerNameEn,
    String buyerNameUz,
    String buyerNameRu,
    String buyerNameEn,
    String productNameUz,
    String productNameRu,
    String productNameEn,
    String templateDataUz,
    String templateDataRu,
    String templateDataEn,
    TemplateStatus status,
    TemplateType templateType,
    VerificationType individualVerificationType,
    VerificationType legalVerificationType,
    Double amount,
    Integer witnessCount,
    String[] enabledCurrencies,
    Boolean productEnabled,
    Boolean productRequired,
    Boolean paymentScheduleEnabled,
    Boolean witnessEnabled,
    Boolean initialPaymentEnabled,
    ExchangeMode exchangeMode,
    RoumingType roumingType,
    Integer priority,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
