package uz.hesap.service.document.model.request;

import java.util.UUID;

public record DocumentValueRequest(
    UUID id,
    Integer position,
    String value,
    UUID templateId,
    UUID templateFieldId,
    String keyName) {}
