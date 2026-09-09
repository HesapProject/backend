package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

/** Response DTO for About content. */
public record AboutResponse(UUID id, TextModel about, Instant createdDate) {}
