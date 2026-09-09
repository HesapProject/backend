package uz.hesap.service.integration.domain.enums;

public enum CardType {
  UZCARD,
  HUMO,
  UNKNOWN;

  public static CardType detect(String cardNumber) {
    if (cardNumber == null) {
      return UNKNOWN;
    }
    if (cardNumber.startsWith("8600")
        || cardNumber.startsWith("5614")
        || cardNumber.startsWith("6262")) {
      return UZCARD;
    } else if (cardNumber.startsWith("9860")) {
      return HUMO;
    }
    return UNKNOWN;
  }
}
