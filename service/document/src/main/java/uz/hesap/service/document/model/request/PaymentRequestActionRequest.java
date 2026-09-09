package uz.hesap.service.document.model.request;

import java.util.UUID;

// To'lov so'rovi ustida amal (approve/reject/cancel) — id body'da.
public record PaymentRequestActionRequest(UUID id) {}
