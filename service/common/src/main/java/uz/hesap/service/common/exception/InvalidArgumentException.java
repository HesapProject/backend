package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class InvalidArgumentException extends RuntimeException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.INVALID_ARGUMENT_ERROR_CODE;

  public InvalidArgumentException(final String message) {
    super(message);
  }

  public InvalidArgumentException(final ErrorCode code, final String message) {
    super(message);
    this.code = code;
  }

  public InvalidArgumentException(String message, IllegalArgumentException e) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }
}
