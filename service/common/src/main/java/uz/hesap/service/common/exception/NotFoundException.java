package uz.hesap.service.common.exception;

import java.util.NoSuchElementException;
import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class NotFoundException extends NoSuchElementException implements ExceptionInterface {
  private ErrorCode code = ErrorCode.NOT_FOUND_ERROR_CODE;

  public NotFoundException(ErrorCode code) {
    super();
    this.code = code;
  }

  public NotFoundException(final ErrorCode code, final String message) {
    super(message);
    this.code = code;
  }

  public NotFoundException(final String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }
}
