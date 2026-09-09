package uz.hesap.service.document.model.request;

import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

public record DelayPaymentApproveRequest(PaymentScheduleStatus status) {}
