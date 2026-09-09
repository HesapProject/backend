package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class BadRequestException extends RuntimeException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.BAD_REQUEST_CODE;

  public BadRequestException(final String message) {
    super(message);
  }

  public BadRequestException(final ErrorCode code, final String message) {
    super(message);
    this.code = code;
  }

  public BadRequestException(final ErrorCode code) {
    super();
    this.code = code;
  }

  public BadRequestException(final String message, final Throwable cause) {
    super(message, cause);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }

  @Override
  public String toString() {
    return "BadRequestException: " + getMessage();
  }
}
