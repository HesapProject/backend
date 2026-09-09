package uz.hesap.service.integration.model.uzcard;

// Plum /UserCard/createUserCard tanasi. `userId` — Plum saqlab, confirm'da echo qiladigan
// identifikator; biz PINFL yuboramiz (karta egasi user_in PINFL bo'lib saqlanadi).
public record PlumCreateCardBody(
    String userId, String cardNumber, String expireDate, String userPhone, String pinfl) {}
