package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.CancelRequestStatus;

public record CancelRequestResponse(
    UUID id,
    UUID contractId,
    String buyerIn,
    String sellerIn,
    String requesterIn,
    CancelRequestStatus status,
    String reason,
    Instant createdDate) {}
