package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;

// Oldi-berdi qatori: shartnoma mahsuloti + hujjat konteksti.
// direction — joriy user nuqtai nazaridan: buyer bo'lsa INCOME, seller bo'lsa OUTCOME.
public record ProductLedgerResponse(
    UUID id,
    UUID documentId,
    String documentNumber,
    DocumentStatus documentStatus,
    Direction direction,
    UserResponse counterpart,
    String name,
    Double amount,
    Map<String, Object> values,
    Currency currency,
    Instant deliveryAt,
    Instant createdDate) {}
