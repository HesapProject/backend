package uz.hesap.service.main.model.request;

import java.util.UUID;
import org.springframework.util.Assert;

public record PermissionActionRequest(
    UUID targetUserId,
    Boolean userInfo,
    Boolean passport,
    Boolean payability,
    Boolean contract,
    Boolean partner) {
  public PermissionActionRequest {
    Assert.notNull(targetUserId, "user id required");
  }
}
