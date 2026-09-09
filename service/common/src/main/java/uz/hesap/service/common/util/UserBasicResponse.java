package uz.hesap.service.common.util;

import java.util.UUID;
import uz.hesap.service.common.util.enums.UserType;

// legalName/type — yuridik shaxs (COMPANY) tomonni ko'rsatish uchun
// (jismoniy shaxsda firstName/lastName, yuridikda legalName ishlatiladi).
public record UserBasicResponse(
    UUID id, String firstName, String lastName, String phone, String legalName, UserType type) {}
