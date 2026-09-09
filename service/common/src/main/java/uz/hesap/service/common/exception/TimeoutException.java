package uz.hesap.service.common.exception;

public class TimeoutException extends RuntimeException {

  public TimeoutException() {}

  public TimeoutException(final String message) {
    super(message);
  }

  public TimeoutException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public TimeoutException(final Throwable cause) {
    super(cause);
  }
}
