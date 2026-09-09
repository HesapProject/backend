package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class AlreadyExistsException extends RuntimeException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.ALREADY_EXISTS_ERROR_CODE;

  public AlreadyExistsException(final ErrorCode code) {
    this.code = code;
  }

  public AlreadyExistsException(final String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }

  public AlreadyExistsException(final ErrorCode code, final String message) {
    super(message);
    this.code = code;
  }
}
