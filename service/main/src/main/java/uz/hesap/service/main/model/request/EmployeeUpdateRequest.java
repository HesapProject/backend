package uz.hesap.service.main.model.request;

import uz.hesap.service.common.util.enums.Role;

public record EmployeeUpdateRequest(String firstName, String lastName, String phone, Role role) {}
