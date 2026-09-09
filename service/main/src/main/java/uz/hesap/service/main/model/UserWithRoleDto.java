package uz.hesap.service.main.model;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.Role;

public record UserWithRoleDto(
    UUID id,
    UUID staffId,
    String firstName,
    String lastName,
    String phone,
    String email,
    Role role,
    Instant createdDate,
    Instant lastModifiedDate) {}
