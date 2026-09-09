package uz.hesap.service.common.exception;

public class LockingConflictException extends RuntimeException {

  public LockingConflictException() {}

  public LockingConflictException(final String message) {
    super(message);
  }

  public LockingConflictException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public LockingConflictException(final Throwable cause) {
    super(cause);
  }

  public LockingConflictException(
      final String message,
      final Throwable cause,
      final boolean enableSuppression,
      final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
