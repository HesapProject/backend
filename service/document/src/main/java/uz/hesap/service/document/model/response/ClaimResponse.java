package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.ClaimStatus;

public record ClaimResponse(
    UUID id,
    UUID contractId,
    String fromIn,
    String toIn,
    String nameUz,
    String nameRu,
    String nameEn,
    Integer number,
    ClaimStatus status,
    String templateJson,
    Instant createdDate) {}
