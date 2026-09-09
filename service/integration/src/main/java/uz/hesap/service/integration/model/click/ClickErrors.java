package uz.hesap.service.integration.model.click;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClickErrors {
  private final Integer error;
  private final String errorNote;

  public static final ClickErrors SUCCESS = new ClickErrors(0, "Success");
  public static final ClickErrors SIGN_CHECK_FAILED = new ClickErrors(-1, "SIGN CHECK FAILED!");
  public static final ClickErrors INCORRECT_PARAMETER_AMOUNT =
      new ClickErrors(-2, "Incorrect parameter amount");
  public static final ClickErrors ACCOUNT_NOT_FOUND = new ClickErrors(-3, "Account not found");
  public static final ClickErrors USER_NOT_FOUND = new ClickErrors(-5, "User not found");
  public static final ClickErrors ALREADY_PAID = new ClickErrors(-4, "Already paid");
  public static final ClickErrors CANCELLED = new ClickErrors(-9, "Transaction cancelled");
  public static final ClickErrors ORDER_NOT_FOUND = new ClickErrors(-5, "Order not found");
  public static final ClickErrors ERROR_IN_REQUEST = new ClickErrors(-7, "Failed to update user");
  public static final ClickErrors TRANSACTION_NOT_FOUND =
      new ClickErrors(-6, "Transaction does not exist");

  public ClickErrors(Integer error, String error_note) {
    this.error = error;
    this.errorNote = error_note;
  }
}
