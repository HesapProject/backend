package uz.hesap.service.document.model.response;

import java.time.Instant;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;

public record WitnessResponse(
    UserResponse user, DocumentWitnessStatus status, Instant lastModifiedDate) {}
