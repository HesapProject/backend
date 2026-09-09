package uz.hesap.service.document.model.request;

import java.time.Instant;

public record C2CPaymentScheduleRequest(Double amount, Instant paymentDate) {}
