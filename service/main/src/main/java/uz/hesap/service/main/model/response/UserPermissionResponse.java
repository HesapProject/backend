package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;

public record UserPermissionResponse(
    UUID id,
    UserBasicResponse userFrom,
    UserBasicResponse userTo,
    Boolean passport,
    Boolean payability,
    Boolean contract,
    Boolean partner,
    Boolean active,
    Instant createdDate,
    Instant lastModifiedDate) {}
