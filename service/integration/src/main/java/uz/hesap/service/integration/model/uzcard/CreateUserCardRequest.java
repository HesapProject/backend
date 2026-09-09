package uz.hesap.service.integration.model.uzcard;

import java.util.UUID;
import org.springframework.util.Assert;

public record CreateUserCardRequest(
    UUID userId, String cardNumber, String expireDate, String userPhone, String pinfl) {
  public CreateUserCardRequest {
    Assert.notNull(userId, "userId required");
    Assert.notNull(cardNumber, "cardNumber required");
    Assert.notNull(expireDate, "expireDate required");
    Assert.notNull(userPhone, "userPhone required");
    Assert.notNull(pinfl, "pinfl required");
    Assert.isTrue(cardNumber.length() == 16, "Invalid card number");
    Assert.isTrue(expireDate.length() == 4, "Invalid card expire");
    Assert.isTrue(cardNumber.chars().allMatch(Character::isDigit), "Invalid card number");
  }
}
