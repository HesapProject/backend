package uz.hesap.service.document.model.request;

import java.util.UUID;

// Shartnomani bekor qilish so'rovini yaratish — contractId + ixtiyoriy sabab.
public record CancelRequestCreateRequest(UUID contractId, String reason) {}
