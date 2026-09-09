package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.TemplateFieldType;
import uz.hesap.service.document.domain.enums.TemplatePosition;

public record TemplateFieldResponse(
    UUID id,
    String parentKey,
    UUID templateId,
    String nameUz,
    String nameRu,
    String nameEn,
    String keyName,
    TemplateFieldType type,
    TemplatePosition position,
    Boolean productField,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
