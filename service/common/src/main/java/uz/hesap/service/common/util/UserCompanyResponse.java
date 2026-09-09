package uz.hesap.service.common.util;

import java.util.UUID;
import java.util.function.Supplier;
import uz.hesap.service.common.util.enums.Role;

public record UserCompanyResponse(UUID userId, Role role, UUID companyId, Boolean isMain)
    implements Supplier<UserCompanyResponse> {
  @Override
  public UserCompanyResponse get() {
    return new UserCompanyResponse(null, null, null, Boolean.FALSE);
  }
}
