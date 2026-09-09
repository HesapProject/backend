package uz.hesap.service.main.model.request;

/**
 * Request DTO for sending SMS verification code Used for both sign-up and password recovery flows
 */
public record SendCodeRequest(
    String firstName, String lastName, String phone, String action // "create" or "recovery"
    ) {
  public static final String ACTION_CREATE = "create";
  public static final String ACTION_RECOVERY = "recovery";

  public SendCodeRequest {
    //        Assert.isTrue(firstName != null && lastName != null && password != null && phone !=
    // null && action != null, "all field required");
  }
}
