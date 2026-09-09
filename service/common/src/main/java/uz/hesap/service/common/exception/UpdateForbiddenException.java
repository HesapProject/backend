package uz.hesap.service.common.exception;

public class UpdateForbiddenException extends ForbiddenException {

  public UpdateForbiddenException() {}

  public UpdateForbiddenException(final String message) {
    super(message);
  }

  public UpdateForbiddenException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public UpdateForbiddenException(final Throwable cause) {
    super(cause);
  }
}
