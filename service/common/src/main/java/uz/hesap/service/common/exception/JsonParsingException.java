package uz.hesap.service.common.exception;

import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.exception.handler.ExceptionInterface;

public class JsonParsingException extends RuntimeException implements ExceptionInterface {
  private final ErrorCode code;

  public JsonParsingException(final ErrorCode code, final String message) {
    super(message);
    this.code = code;
  }

  @Override
  public ErrorCode getCode() {
    return code;
  }
}
