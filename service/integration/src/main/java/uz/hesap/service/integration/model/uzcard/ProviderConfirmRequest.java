package uz.hesap.service.integration.model.uzcard;

public record ProviderConfirmRequest(Integer session, String otp, Integer isTrusted) {}
