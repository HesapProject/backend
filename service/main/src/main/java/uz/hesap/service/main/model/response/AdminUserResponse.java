package uz.hesap.service.main.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.enums.CompanyType;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.SessionEntity;

public record AdminUserResponse(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String midName,
    String phone,
    String email,
    String in,
    String passport,
    UserType type,
    Role role,
    String legalName,
    String tin,
    Boolean isVerified,
    Boolean deleted,
    Instant createdDate,
    // Ro'yxat (admin) uchun agregatlar — cross-schema query'dan to'ldiriladi.
    // Detal/update'da null bo'ladi.
    Integer contractsCount,
    Double balance,
    Instant lastVisitDate,
    List<CompanyInfo> companies,
    List<SessionEntity> sessions) {
  public record CompanyInfo(UUID id, String name, String customName, CompanyType type) {}
}
