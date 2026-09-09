package uz.hesap.service.main.model;

import uz.hesap.service.common.util.TextModel;

/** Request DTO for creating legal documents (privacy/terms). */
public record TermsRequest(TextModel terms) {}
