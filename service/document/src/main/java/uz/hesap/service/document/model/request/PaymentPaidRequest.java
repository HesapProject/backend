package uz.hesap.service.document.model.request;

import java.util.UUID;

// proof — to'lov cheki rasm URL (ixtiyoriy).
public record PaymentPaidRequest(UUID paymentId, Double amount, String proof) {}
