package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.enums.TemplateDistributionType;

// aktiv paket to'liq ma'lumoti (snapshot). templates — paketdan beriladigan shartnoma
// turlari va sonlari (config'dan); templateType/totalCount — SHARED pool uchun.
public record UserPackageDetailResponse(
    UUID balanceId,
    String type,
    TextModel name,
    TextModel description,
    Double price,
    Integer stars,
    Integer duration,
    Instant expireDate,
    Instant purchasedDate,
    TemplateDistributionType templateType,
    Integer totalCount,
    Integer scoringHesap,
    Integer scoringKatm,
    Integer scoringPayment,
    Integer scoringPaymentUsed,
    List<UserTemplateUsageResponse> templates) {}
