package uz.hesap.api.gateway.exception;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import org.immutables.builder.Builder;
import org.springframework.http.HttpStatus;

public record ExceptionResponse(
    int code, String status, String path, String message, String timestamp) {

  public static final int UNAUTHORIZED_ERROR_CODE = 20;

  @Builder.Constructor
  public ExceptionResponse(
      final Exception exception, final int code, final String path, final HttpStatus status) {
    this(
        code,
        String.format("%d %s", status.value(), status.getReasonPhrase()),
        path,
        exception.getMessage(),
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
            .withZone(ZoneId.of("+5"))
            .format(Instant.now()));
  }

  public ExceptionResponse(
      final int code, final String errorMsg, final String path, final HttpStatus status) {
    this(
        code,
        String.format("%d %s", status.value(), status.getReasonPhrase()),
        path,
        errorMsg,
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
            .withZone(ZoneId.of("UTC+5"))
            .format(Instant.now()));
  }
}
