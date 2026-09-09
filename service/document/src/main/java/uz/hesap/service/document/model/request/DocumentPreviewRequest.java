package uz.hesap.service.document.model.request;

import java.util.List;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.Currency;

/**
 * Shablon + to'ldirilgan qiymatlar bo'yicha PDF preview so'rovi. Hujjat saqlanmaydi — faqat
 * Jasper orqali render qilinadi. `values` frontend'dagi flat qiymatlar bilan bir xil shaklda.
 *
 * <p>buyerUserId/sellerUserId/price/currency — ixtiyoriy. Berilsa, preview dummy o'rniga
 * haqiqiy taraf (DB'dan) va kiritilgan summa/valyutani ko'rsatadi.
 */
public record DocumentPreviewRequest(
    UUID templateId,
    String number,
    List<PreviewValue> values,
    // Taraflar BARQAROR identifikatori (PINFL/STIR). Yuboruvchi tomon bo'sh bo'lsa
    // principal identifier'i bilan to'ldiriladi (create bilan bir xil).
    String buyerIn,
    String sellerIn,
    Double price,
    Currency currency,
    List<DocumentRequest.PaymentItem> payments,
    // Preview'da $P{products} jadvali shundan quriladi — berilmasa bo'sh ro'yxat.
    List<ContractProductRequest> products,
    // Ko'rish tili (uz/ru/en) — preview shu tilda render qilinadi. null bo'lsa uz.
    String lang) {

  public record PreviewValue(UUID templateFieldId, String value, Integer position) {}
}
