package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPurpose;
import uz.hesap.service.document.domain.enums.DocumentStatus;

public record DocumentRequest(
    // Taraflar BARQAROR identifikatori (PINFL/STIR) bilan beriladi — user_id emas
    // (user_id beqaror/null bo'lib 500 berardi). Yuboruvchi tomon bo'sh bo'lsa,
    // ContractsService principal.identifier() bilan to'ldiradi.
    String buyerIn,
    String sellerIn,
    UUID templateId,
    UUID userPackageId,
    String number,
    DocumentStatus status,
    // Hujjat maqsadi — bo'sh bo'lsa CONTRACT (ContractsService.create'da default).
    DocumentPurpose purpose,
    Double price,
    Currency currency,
    UUID currencyId,
    Double initialPayment,
    Instant deliveryAt,
    List<DocumentValueRequest> values,
    List<PaymentItem> payments,
    List<UUID> witnessIds,
    List<ContractProductRequest> products) {

  // To'lov jadvali qatori: summa + sana.
  public record PaymentItem(Double amount, Instant paymentDate) {}
}
