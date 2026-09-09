package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentStatus;

public record DocumentResponse(
    UUID id,
    UUID buyerUserId,
    UUID sellerUserId,
    UUID templateId,
    String number,
    DocumentStatus status,
    Double price,
    Currency currency,
    UUID currencyId,
    Double initialPayment,
    Instant deliveryAt,
    List<DocumentValueResponse> values,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate,
    Long version) {}
