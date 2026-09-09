package uz.hesap.service.main.model.request;

import uz.hesap.service.common.util.enums.Role;

public record EmployeeRequest(
    String firstName, String lastName, String phone, String password, Role role) {}
