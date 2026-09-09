package uz.hesap.service.integration.model.uzcard;

// Orphan karta tozalash so'rovi: PINFL (user identifikatori) + karta raqami.
public record CleanOrphanCardRequest(String pinfl, String cardNumber) {}
