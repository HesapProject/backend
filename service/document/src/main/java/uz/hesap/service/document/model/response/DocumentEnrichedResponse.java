package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.DocumentPurpose;
import uz.hesap.service.document.domain.enums.DocumentStatus;

// boyitilgan (enriched) hujjat javobi — user, template ma'lumotlari bilan
public record DocumentEnrichedResponse(
    UUID id,
    UUID buyerUserId,
    UUID sellerUserId,
    UUID templateId,
    String number,
    DocumentStatus status,
    // Hujjat maqsadi — CONTRACT/TTN/AKT/FACTURA.
    DocumentPurpose purpose,
    // Taraf imzo holatlari — kim imzolagani/kutilayotgani statusdan emas, shulardan aniqlanadi.
    DocumentPartyStatus buyerStatus,
    DocumentPartyStatus sellerStatus,
    Double price,
    Currency currency,
    UUID currencyId,
    Double initialPayment,
    Instant deliveryAt,
    // Taraflarning barqaror identifikatori (PINFL/STIR) — hujjatda saqlanadi,
    // user o'chsa/o'zgarsa ham qoladi (UserBasicResponse'da `in` yo'q).
    String buyerIn,
    String sellerIn,
    // enriched fields — tashqi servisdan olingan ma'lumotlar
    UserBasicResponse buyer,
    UserBasicResponse seller,
    TemplateBasicResponse template,
    List<DocumentValueResponse> values,
    // Sotuv shartnomasi mahsulotlari (getById'da to'ldiriladi; list'da bo'sh).
    List<ContractProductResponse> products,
    Boolean deleted,
    // Kim yaratgan: PINFL/STIR (creator_in) + enriched user (ism). Yuridik shaxsda yaratuvchi
    // buyer/seller bo'lmasligi mumkin, shuning uchun ism alohida enrich qilinadi.
    String creatorIn,
    UserBasicResponse createdBy,
    Instant createdDate,
    Instant lastModifiedDate,
    Long version) {}
