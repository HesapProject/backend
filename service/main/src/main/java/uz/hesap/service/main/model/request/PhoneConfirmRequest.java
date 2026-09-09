package uz.hesap.service.main.model.request;

// Minimal request: no extra validation/checks here by design.
public record PhoneConfirmRequest(String phone, String code) {}
