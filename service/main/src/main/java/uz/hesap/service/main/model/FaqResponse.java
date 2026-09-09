package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

/**
 * Response DTO for FAQ entries. Returns FAQ data with multilingual support.
 *
 * @param id Unique identifier of the FAQ
 * @param title FAQ question/title in multiple languages
 * @param answer FAQ answer in multiple languages
 * @param createdDate Timestamp when the FAQ was created
 * @author elmurod
 * @since 2026-02-07
 */
public record FaqResponse(UUID id, TextModel title, TextModel answer, Instant createdDate) {}
