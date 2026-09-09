package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

/** B2B to'langan to'lovlar — enriched response. */
public record B2BPaidScheduleResponse(
    UUID id,
    UUID documentId,
    String documentNumber,
    UUID paymentScheduleId,
    UserBasicResponse buyer,
    UserBasicResponse seller,
    PaymentScheduleStatus status,
    Currency currency,
    UUID currencyId,
    Double amount,
    Instant paymentDate,
    Instant createdDate) {}
