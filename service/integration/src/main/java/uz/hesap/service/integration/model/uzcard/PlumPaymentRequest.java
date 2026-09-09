package uz.hesap.service.integration.model.uzcard;

import java.util.UUID;

// Kartadan pul yechib balansga qo'shish so'rovi (ichki API).
// cardId — billing.plum_cards dagi UUID; egasi (userId) va Plum karta id'si shu yozuvdan olinadi.
public record PlumPaymentRequest(UUID cardId, Double amount, String transactionData) {}
