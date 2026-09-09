package uz.hesap.service.document.model.response;

public record PaymentScoreResponse(
    long unpaid, long earlyPaid, long onTime, long late, long veryLate, long extended) {}
