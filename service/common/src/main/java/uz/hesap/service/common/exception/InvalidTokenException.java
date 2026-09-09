package uz.hesap.service.common.exception;

public class InvalidTokenException extends UnauthorizedException {

  public InvalidTokenException() {}

  public InvalidTokenException(final String message) {
    super(message);
  }

  public InvalidTokenException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
