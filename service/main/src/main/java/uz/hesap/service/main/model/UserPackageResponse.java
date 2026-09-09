package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;

public record UserPackageResponse(
    UUID id,
    UUID packageId,
    String userIn,
    Instant expDate,
    Instant createdAt,
    Instant updatedAt) {}
