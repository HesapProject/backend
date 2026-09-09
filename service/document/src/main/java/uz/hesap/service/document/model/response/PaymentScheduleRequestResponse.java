package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

public record PaymentScheduleRequestResponse(
    UUID id,
    String buyerIn,
    String sellerIn,
    String creatorIn,
    UUID contractId,
    PaymentScheduleStatus status,
    UUID paymentId,
    Double amount,
    uz.hesap.service.document.domain.enums.Currency currency,
    Instant paymentDate,
    String note,
    String image,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
