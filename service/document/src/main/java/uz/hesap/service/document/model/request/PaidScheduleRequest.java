package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

public record PaidScheduleRequest(
    UUID id,
    UUID buyerId,
    UUID sellerId,
    UUID documentId,
    UUID paymentScheduleId,
    PaymentScheduleStatus status,
    Double amount,
    Instant paymentDate) {}
