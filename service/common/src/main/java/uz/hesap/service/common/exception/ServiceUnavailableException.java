package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class ServiceUnavailableException extends RuntimeException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.BAD_REQUEST_CODE;

  public ServiceUnavailableException() {}

  public ServiceUnavailableException(final String message) {
    super(message);
  }

  public ServiceUnavailableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ServiceUnavailableException(final Throwable cause) {
    super(cause);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }
}
