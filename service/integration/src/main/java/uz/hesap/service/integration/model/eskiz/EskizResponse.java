package uz.hesap.service.integration.model.eskiz;

/** Eskiz API response */
public record EskizResponse(
    String id, String code, String message, EskizTokenData data, String status) {
  public record EskizTokenData(String token) {}
}
