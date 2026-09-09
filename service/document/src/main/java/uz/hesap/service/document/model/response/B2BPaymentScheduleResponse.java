package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

/** Payment — enriched response (user ma'lumotlari bilan). */
public record B2BPaymentScheduleResponse(
    UUID id,
    UUID contractId,
    String documentNumber,
    UserBasicResponse buyer,
    UserBasicResponse seller,
    PaymentScheduleStatus status,
    DocumentStatus contractStatus,
    Double totalAmount,
    Double paidAmount,
    Instant contractPaymentDate,
    Instant changedPaymentDate,
    Instant paidAt,
    Currency currency,
    UUID currencyId,
    Instant createdAt) {}
