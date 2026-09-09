package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

// Haqdorga kelgan to'lov so'rovi — shartnoma raqami va yuborgan user bilan
// (So'rovlarim sahifasi uchun).
public record IncomingPaymentRequestResponse(
    UUID id,
    UUID documentId,
    String documentNumber,
    UserBasicResponse user,
    PaymentScheduleStatus status,
    UUID paymentScheduleId,
    Double amount,
    Instant paymentDate,
    String note,
    String image,
    Instant createdDate) {}
