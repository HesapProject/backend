package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

public record BannerResponse(
    UUID id,
    TextModel title,
    String image,
    String link,
    Integer sortOrder,
    Boolean isActive,
    Instant createdDate) {}
