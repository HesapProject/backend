package uz.hesap.service.integration.model.uzcard;

import org.springframework.util.Assert;

public record ConfirmUserCardRequest(Integer session, String otp) {

  public ConfirmUserCardRequest {
    Assert.notNull(session, "session is required");
    Assert.notNull(otp, "otp is required");
  }
}
