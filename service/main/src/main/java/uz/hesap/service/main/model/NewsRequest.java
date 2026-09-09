package uz.hesap.service.main.model;

import java.time.Instant;
import uz.hesap.service.common.util.TextModel;

/** Request DTO for creating/updating blog posts. */
public record NewsRequest(
    TextModel title, TextModel body, String image, Instant date, Boolean isHome) {}
