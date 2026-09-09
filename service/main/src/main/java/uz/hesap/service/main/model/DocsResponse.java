package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.LegalDocumentType;

// Hujjat javobi — type (ABOUT/TERMS/PRIVACY) + content.
public record DocsResponse(UUID id, LegalDocumentType type, TextModel content, Instant createdDate) {}
