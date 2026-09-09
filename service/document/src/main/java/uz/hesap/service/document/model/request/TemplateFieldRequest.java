package uz.hesap.service.document.model.request;

import java.util.UUID;
import uz.hesap.service.document.domain.enums.TemplateFieldType;
import uz.hesap.service.document.domain.enums.TemplatePosition;

public record TemplateFieldRequest(
    UUID id,
    String parentKey,
    UUID templateId,
    String nameUz,
    String nameRu,
    String nameEn,
    String keyName,
    TemplateFieldType type,
    TemplatePosition position,
    Boolean productField) {}
