package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.SignatureActionType;

// Hujjat imzolari — Log tab uchun (kim, qachon, qanday action).
public record DocumentSignatureResponse(
    UUID id,
    UUID documentId,
    UUID userId,
    SignatureActionType actionType,
    Instant createdDate) {}
