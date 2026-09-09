package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

// Kechiktirish so'rovlari ro'yxati — PaymentScheduleRequestResponse maydonlari +
// ro'yxatda ko'rsatish uchun boyitma: shartnoma raqami, so'rovchi ism-sharifi,
// nechanchi to'lov (paymentOrder/paymentsTotal).
public record DelayRequestListResponse(
    UUID id,
    String buyerIn,
    String sellerIn,
    String creatorIn,
    UUID contractId,
    PaymentScheduleStatus status,
    UUID paymentId,
    Double amount,
    Currency currency,
    Instant paymentDate,
    String note,
    String contractNumber,
    String requesterName,
    Integer paymentOrder,
    Integer paymentsTotal,
    Instant createdDate,
    Instant lastModifiedDate) {}
