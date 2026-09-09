package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

public record PaidScheduleResponse(
    UUID id,
    UserBasicResponse buyer,
    UserBasicResponse seller,
    UUID documentId,
    UUID paymentScheduleId,
    PaymentScheduleStatus status,
    Double amount,
    Instant paymentDate,
    String proof,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate) {}
