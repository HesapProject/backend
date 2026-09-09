package uz.hesap.service.log.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import uz.hesap.service.common.util.enums.Activity;

public record ActivityLogResponse(
    UUID id,
    String actorIn,
    String companyIn,
    Activity activity,
    Map<String, Object> data,
    Instant createdDate) {}
