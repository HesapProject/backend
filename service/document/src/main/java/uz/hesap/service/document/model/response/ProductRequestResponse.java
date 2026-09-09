package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;

public record ProductRequestResponse(
    UUID id,
    UUID contractId,
    UUID productId,
    String buyerIn,
    String sellerIn,
    String requesterIn,
    ProductRequestStatus status,
    Double quantity,
    Double amount,
    String reason,
    Instant createdDate) {}
