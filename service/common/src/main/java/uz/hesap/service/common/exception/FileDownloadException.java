package uz.hesap.service.common.exception;

public class FileDownloadException extends RuntimeException {

  public FileDownloadException() {}

  public FileDownloadException(final String message) {
    super(message);
  }

  public FileDownloadException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
