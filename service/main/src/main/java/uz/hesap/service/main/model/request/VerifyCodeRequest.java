package uz.hesap.service.main.model.request;

import org.springframework.util.Assert;

/**
 * Request DTO for verifying SMS code. Only requires phone and code - no password or device userInfo
 * needed at this step.
 */
public record VerifyCodeRequest(String phone, String code) {
  public VerifyCodeRequest {
    Assert.hasLength(phone, "phone can't be null or empty");
    Assert.hasLength(code, "code can't be null or empty");
  }
}
