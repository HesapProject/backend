package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;

/** Response DTO for legal documents (privacy/terms). */
public record TermsResponse(UUID id, TextModel terms, Instant createdDate) {}
