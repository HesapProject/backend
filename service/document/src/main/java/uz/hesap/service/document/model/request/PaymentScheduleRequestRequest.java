package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.UUID;

// buyerCompanyId/sellerCompanyId/userId DocumentEntity dan olinadi.
// note — izoh, image — chek rasmi (CDN URL yoki base64) — to'lov so'roviga biriktiriladi.
public record PaymentScheduleRequestRequest(
    UUID id, UUID paymentScheduleId, Double amount, Instant paymentDate, String note, String image) {}
