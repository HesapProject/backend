package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class ForbiddenException extends RuntimeException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.FORBIDDEN_ERROR_CODE;

  public ForbiddenException() {}

  public ForbiddenException(final String message) {
    super(message);
  }

  public ForbiddenException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ForbiddenException(final Throwable cause) {
    super(cause);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }
}
