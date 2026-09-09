package uz.hesap.service.common.exception;

public class ListForbiddenException extends ForbiddenException {

  public ListForbiddenException() {}

  public ListForbiddenException(final String message) {
    super(message);
  }

  public ListForbiddenException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ListForbiddenException(final Throwable cause) {
    super(cause);
  }
}
