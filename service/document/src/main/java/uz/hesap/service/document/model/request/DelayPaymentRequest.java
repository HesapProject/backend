package uz.hesap.service.document.model.request;

import java.time.Instant;

public record DelayPaymentRequest(Instant date, String note) {}
