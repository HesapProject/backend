package uz.hesap.service.main.model.response;

import java.util.UUID;
import uz.hesap.service.common.util.enums.UserType;

// Admin login javobi. adminId — endi UserEntity id (admin user). type — ADMIN | SUPER_ADMIN.
public record AdminAuthResponse(
    String token, UUID adminId, String login, UserType type, String fullName) {}
