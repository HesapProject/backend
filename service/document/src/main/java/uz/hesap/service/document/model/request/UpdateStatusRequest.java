package uz.hesap.service.document.model.request;

import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;

public record UpdateStatusRequest(PaymentScheduleStatus status) {}
