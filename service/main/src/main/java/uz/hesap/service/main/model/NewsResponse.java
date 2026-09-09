package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

/** Response DTO for blog posts. */
public record NewsResponse(
    UUID id,
    TextModel title,
    TextModel body,
    String image,
    Instant date,
    Boolean isHome,
    Boolean isSend) {}
