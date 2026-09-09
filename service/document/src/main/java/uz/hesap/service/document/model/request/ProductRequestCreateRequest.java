package uz.hesap.service.document.model.request;

import java.util.UUID;

// Mahsulot so'rovini yaratish — contractId + productId + berilgan soni + ixtiyoriy sabab.
// quantity — berilgan SONI (product quantity'sidan kam bo'lsa qisman). amount legacy.
public record ProductRequestCreateRequest(
    UUID contractId, UUID productId, Double quantity, Double amount, String reason) {}
