package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentValueResponse(
    UUID id,
    Integer position,
    String value,
    UUID documentId,
    UUID templateId,
    UUID templateFieldId,
    String keyName,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
