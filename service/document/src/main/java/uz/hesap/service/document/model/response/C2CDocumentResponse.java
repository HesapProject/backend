package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.DocumentPurpose;
import uz.hesap.service.document.domain.enums.DocumentStatus;

public record C2CDocumentResponse(
    UUID id,
    UserResponse buyer,
    UserResponse seller,
    String number,
    DocumentStatus status,
    // Hujjat maqsadi — CONTRACT/TTN/AKT/FACTURA.
    DocumentPurpose purpose,
    // Taraf imzo holatlari — kim imzolagani/kutilayotgani shulardan aniqlanadi.
    DocumentPartyStatus buyerStatus,
    DocumentPartyStatus sellerStatus,
    Double price,
    Currency currency,
    UUID currencyId,
    Double initialPayment,
    Instant deliveryAt,
    List<WitnessResponse> witnesses,
    List<ContractProductResponse> products,
    // Shartnomani bekor qilish so'rovlari (detalda — getById'da to'ldiriladi).
    List<CancelRequestResponse> cancelRequests,
    Object documentContent,
    Boolean deleted,
    Instant createdDate,
    Instant lastModifiedDate,
    Long version) {}
