package uz.hesap.service.integration.model.uzcard;

public record ConfirmUserCardResponse(Result result, ErrorResponse error) {
  public record Result(Card card) {}

  public record Card(
      Long id,
      String userId,
      Long cardId,
      String number,
      Double balance,
      Integer status,
      String expireDate) {}
}
