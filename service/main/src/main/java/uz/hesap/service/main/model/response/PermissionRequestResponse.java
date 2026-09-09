package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.main.domain.PermissionRequestStatus;

public record PermissionRequestResponse(
    UUID id,
    UserBasicResponse userFrom,
    UserBasicResponse userTo,
    Boolean passport,
    Boolean payability,
    Boolean contract,
    Boolean partner,
    PermissionRequestStatus status,
    Instant createdDate,
    Instant lastModifiedDate) {}
