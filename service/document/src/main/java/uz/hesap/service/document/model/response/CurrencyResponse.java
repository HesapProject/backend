package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;

public record CurrencyResponse(
    UUID id,
    String code,
    String nameUz,
    String nameRu,
    String nameEn,
    String symbol,
    Integer sortOrder,
    Boolean isActive,
    Instant createdDate,
    Instant lastModifiedDate) {}
