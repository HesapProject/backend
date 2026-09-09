package uz.hesap.service.main.model.eskiz;

public record EskizResponse(
    String id, String code, String message, TokenResponse data, String status) {}
