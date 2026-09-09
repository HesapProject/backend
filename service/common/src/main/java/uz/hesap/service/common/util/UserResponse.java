package uz.hesap.service.common.util;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;

@Builder
public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String midName,
    String phone,
    String username,
    String document,
    UserType type,
    Boolean verified,
    String email,
    Role role,
    String in,
    String legalName,
    String tin,
    String address,
    String region,
    String district,
    String photo,
    String birthday,
    String birthPlace,
    String passportIssuedBy,
    String passportIssueDate,
    String passportExpiryDate,
    String nationality,
    String citizenship,
    SessionResponse device,
    CompanyResponse company,
    Instant createdDate,
    Instant lastModifiedDate) {
  public String fullName() {
    return Utils.getNotNull(firstName, "") + " " + Utils.getNotNull(lastName, "");
  }

  // Active kompaniya id'si — kompaniya konteksti bo'lmasa (CLIENT/COMPANY
  // self-mode yoki active kompaniya hal qilinmagan) null qaytaradi.
  // company().id() ni to'g'ridan-to'g'ri chaqirish NPE bermasligi uchun.
  public UUID companyId() {
    return company != null ? company.id() : null;
  }

  // Barqaror identifikator: jismoniy uchun PINFL (`in`), yuridik uchun STIR (`tin`).
  // Shartnomalarni shu bo'yicha bog'lash uchun (user o'chsa ham o'zgarmaydi).
  public String identifier() {
    return (in != null && !in.isBlank()) ? in : tin;
  }
}
