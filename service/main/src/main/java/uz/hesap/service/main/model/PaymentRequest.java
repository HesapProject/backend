package uz.hesap.service.main.model;

import java.util.UUID;
import uz.hesap.service.common.util.enums.PaymentMethod;

// Qo'lda to'lov yozish/yangilash (admin). createdBy/updatedBy controller'da principal'dan.
public record PaymentRequest(Double amount, UUID userId, PaymentMethod paymentMethod) {}
