package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

// buyerId/sellerId/companyId lar DocumentEntity dan olinadi — frontenddan kelmaydi
public record PaymentScheduleRequest(
    UUID id, UUID documentId, PaymentScheduleStatus status, Double amount, Instant paymentDate) {}
