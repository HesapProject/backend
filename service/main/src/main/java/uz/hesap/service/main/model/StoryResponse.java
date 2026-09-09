package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

public record StoryResponse(
    UUID id,
    TextModel title,
    String avatar,
    List<StoryItem> items,
    Boolean isViewed,
    Instant createdDate) {}
