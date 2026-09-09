package uz.hesap.service.integration.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.domain.enums.TransactionType;

// Balans tranzaksiyasi (ledger qatori). amount ishorali: + to'ldirish, − yechish.
public record TransactionResponse(
    UUID id,
    Double amount,
    TransactionType type,
    BillingType billingType,
    String description,
    Instant timestamp) {}
