package uz.hesap.service.document.api.openapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.model.request.ContractProductRequest;
import uz.hesap.service.document.model.request.DocumentRequest;
import uz.hesap.service.document.model.request.DocumentValueRequest;

/**
 * Public API orqali shartnoma yaratish so'rovi.
 *
 * <p>Taraflardan BIRI kalit egasi bo'lishi shart: `buyerIn` yoki `sellerIn` bo'sh qoldirilsa u
 * yerga kalit egasining identifikatori qo'yiladi. Hujjat raqami (number) backendda
 * generatsiya qilinadi, status esa har doim CREATED — ularni so'rovda berib bo'lmaydi.
 */
@Schema(name = "ContractRequest", description = "Shartnoma yaratish so'rovi")
public record OpenApiContractRequest(
    @Schema(description = "Shablon id — kalitga ruxsat etilgan bo'lishi shart", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID templateId,
    @Schema(description = "Xaridor PINFL/STIR (bo'sh -> kalit egasi)") String buyerIn,
    @Schema(description = "Sotuvchi PINFL/STIR (bo'sh -> kalit egasi)") String sellerIn,
    @Schema(description = "Shartnoma summasi") Double price,
    @Schema(description = "Valyuta kodi: UZS/USD/RUB") Currency currency,
    @Schema(description = "Boshlang'ich to'lov") Double initialPayment,
    @Schema(description = "Yetkazib berish sanasi") Instant deliveryAt,
    @Schema(description = "Shablon maydonlari qiymatlari") List<DocumentValueRequest> values,
    @Schema(description = "To'lov jadvali") List<PaymentItem> payments,
    @Schema(description = "Guvohlar (user id)") List<UUID> witnessIds,
    @Schema(description = "Mahsulotlar") List<ContractProductRequest> products) {

  @Schema(name = "PaymentItem", description = "To'lov jadvali qatori")
  public record PaymentItem(Double amount, Instant paymentDate) {}

  // Ichki DocumentRequest'ga o'girish. number/status/purpose/userPackageId — backend hal qiladi.
  public DocumentRequest toDocumentRequest() {
    return new DocumentRequest(
        buyerIn,
        sellerIn,
        templateId,
        null,
        null,
        null,
        null,
        price,
        currency,
        null,
        initialPayment,
        deliveryAt,
        values,
        payments == null
            ? null
            : payments.stream()
                .map(p -> new DocumentRequest.PaymentItem(p.amount(), p.paymentDate()))
                .toList(),
        witnessIds,
        products);
  }
}
