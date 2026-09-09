package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.UserType;

// Control admin ro'yxati/yaratish javobi (parol qaytarilmaydi).
public record AdminResponse(
    UUID id,
    String firstName,
    String lastName,
    String login,
    UserType type,
    Instant createdDate) {}
