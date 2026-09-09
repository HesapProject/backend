package uz.hesap.service.common.exception;

public class ApiException extends RuntimeException {
  protected String errorCode;

  public String getErrorCode() {
    return errorCode;
  }

  public ApiException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }
}
