package uz.hesap.service.integration.model.uzcard;

public record DeleteUserCardResponse(Result result, Object error) {
  public record Result(Boolean success) {}
}
