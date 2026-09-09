package uz.hesap.service.main.model.response;

import java.util.UUID;

// Tasdiqlangan telefon va token'dagi user id.
public record PhoneConfirmResponse(UUID id, String phone) {}
