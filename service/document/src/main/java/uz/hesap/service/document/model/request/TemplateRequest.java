package uz.hesap.service.document.model.request;

import java.util.UUID;
import org.springframework.util.Assert;
import uz.hesap.service.document.domain.enums.ExchangeMode;
import uz.hesap.service.document.domain.enums.RoumingType;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.domain.enums.VerificationType;

public record TemplateRequest(
    UUID companyId,
    String nameUz,
    String nameRu,
    String nameEn,
    String sellerNameUz,
    String sellerNameRu,
    String sellerNameEn,
    String buyerNameUz,
    String buyerNameRu,
    String buyerNameEn,
    // Mahsulot nomi/yorlig'i (taraf nomlaridek, 3 til).
    String productNameUz,
    String productNameRu,
    String productNameEn,
    UUID templateId,
    // JRXML manbasi 3 tilda (nullable — mavjud bo'lmagan tilni yubormaslik mumkin).
    String templateDataUz,
    String templateDataRu,
    String templateDataEn,
    TemplateStatus status,
    TemplateType templateType,
    VerificationType individualVerificationType,
    VerificationType legalVerificationType,
    Double amount,
    Integer witnessCount,
    // Yo'l qo'yilgan valyutalar kodi ro'yxati. NULL/empty → barcha valyutalar.
    String[] enabledCurrencies,
    // Mahsulotlar va to'lov jadvali yoqilganmi (NULL → o'zgartirilmaydi).
    Boolean productEnabled,
    // Mahsulotlar majburiymi (yoqilgan bo'lsa kamida 1 ta shart).
    Boolean productRequired,
    Boolean paymentScheduleEnabled,
    // Guvohlik yoqilganmi (Bor/Yo'q).
    Boolean witnessEnabled,
    // Boshlang'ich to'lov yoqilganmi (Bor/Yo'q). Summa shartnomada qoladi.
    Boolean initialPaymentEnabled,
    // Oldi-berdi turi: GOODS (mahsulot/soni) yoki MONEY (qarz/summa).
    ExchangeMode exchangeMode,
    // Rouming hujjat turi (NULL -> yuborilmaydi).
    RoumingType roumingType,
    // Ko'rsatish tartibi — katta qiymat oldinroq chiqadi (NULL → o'zgartirilmaydi, edit'da).
    Integer priority) {
  public TemplateRequest {
    Assert.isTrue(
        nameUz != null && nameEn != null && nameRu != null && templateType != null,
        "name and type required");
    if (templateType == TemplateType.NOTICE) {
      Assert.notNull(templateId != null, "template required for notice");
    }
  }
}
