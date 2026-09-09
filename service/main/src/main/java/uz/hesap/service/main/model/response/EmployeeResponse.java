package uz.hesap.service.main.model.response;

import java.util.UUID;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;

public record EmployeeResponse(
    UUID id, String firstName, String lastName, String phone, UserType type, Role role) {}
