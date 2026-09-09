package uz.hesap.service.common.exception;

public class InvalidCredentialsException extends UnauthorizedException {

  public InvalidCredentialsException() {}

  public InvalidCredentialsException(final String message) {
    super(message);
  }

  public InvalidCredentialsException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public InvalidCredentialsException(final Throwable cause) {
    super(cause);
  }
}
