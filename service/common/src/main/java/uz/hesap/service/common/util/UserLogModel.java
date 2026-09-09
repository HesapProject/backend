package uz.hesap.service.common.util;

import java.util.Map;
import java.util.UUID;
import uz.hesap.service.common.util.enums.UserLogReason;

public record UserLogModel(
    UUID userId,
    UserLogReason reason,
    String firstName,
    String lastName,
    Map<String, Object> oldVersion,
    Map<String, Object> newVersion) {

  public static UserLogModel build(
      UserPrincipal userPrincipal,
      Map<String, Object> oldVersion,
      Map<String, Object> newVersion,
      UserLogReason reason) {
    return new UserLogModel(
        userPrincipal.user().id(),
        reason,
        userPrincipal.user().firstName(),
        userPrincipal.user().lastName(),
        oldVersion,
        newVersion);
  }
}
