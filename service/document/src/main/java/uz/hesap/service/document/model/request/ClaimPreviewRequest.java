package uz.hesap.service.document.model.request;

import java.util.UUID;

// report preview so'rovi — faqat documentId kerak
public record ClaimPreviewRequest(UUID documentId) {}
