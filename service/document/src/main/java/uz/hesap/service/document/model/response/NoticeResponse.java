package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.NoticeStatus;

public record NoticeResponse(
    UUID id,
    UUID contractId,
    String sellerIn,
    String buyerIn,
    String nameUz,
    String nameRu,
    String nameEn,
    Integer number,
    NoticeStatus status,
    String templateJson,
    Instant createdDate) {}
