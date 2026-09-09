package uz.hesap.service.integration.model;

import java.util.UUID;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.integration.domain.enums.PurchaseType;

// Qo'lda xarid yozish/yangilash (admin). createdBy/updatedBy controller'da principal'dan.
public record PurchaseRequest(
    Double amount,
    String userIn,
    String promo,
    PaymentMethod paymentMethod,
    PurchaseType unitType,
    UUID unitId) {}
