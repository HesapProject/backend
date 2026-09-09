package uz.hesap.service.document.model.response;

import uz.hesap.service.document.domain.enums.Currency;

/** B2B to'lov statistikasi — currency bo'yicha. */
public record B2BPaymentStatsResponse(Currency currency, long total, long pending, long paid) {}
