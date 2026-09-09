package uz.hesap.service.common.exception.handler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * API xatolik javobi. - code: ErrorCode.value (raqamli kod — frontend i18n uchun) - status: HTTP
 * status (masalan "404 Not Found") - path: so'rov yo'li - message: exception dagi xabar (log uchun
 * / developer uchun) - description: i18n dan olingan foydalanuvchi uchun xabar (tilga mos) -
 * timestamp: xatolik vaqti
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExceptionResponse(
    int code, String status, String path, String message, String timestamp, String description) {

  /** i18n description siz constructor (eski moslik uchun). */
  public ExceptionResponse(
      final Exception exception,
      final String path,
      final org.springframework.http.HttpStatus status,
      final String description) {
    this(
        findErrorCode(exception),
        String.format("%d %s", status.value(), status.getReasonPhrase()),
        path,
        exception.getMessage(),
        formatTimestamp(),
        description);
  }

  /** i18n description bilan constructor (yangi — GlobalExceptionHandler ishlatadi). */
  public ExceptionResponse(
      final Exception exception,
      final String path,
      final org.springframework.http.HttpStatus status,
      final ErrorCode errorCode,
      final String i18nDescription) {
    this(
        errorCode.value,
        String.format("%d %s", status.value(), status.getReasonPhrase()),
        path,
        resolveMessage(exception),
        formatTimestamp(),
        i18nDescription);
  }

  // exception message null bo'lsa — class name qaytarish (NPE va boshqa null-message
  // exceptionlar uchun "message: null" o'rniga "NullPointerException" ko'rinadi)
  private static String resolveMessage(final Exception e) {
    if (e.getMessage() != null) return e.getMessage();
    return e.getClass().getSimpleName();
  }

  private static String formatTimestamp() {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
        .withZone(ZoneId.of("UTC+5"))
        .format(Instant.now());
  }

  private static int findErrorCode(final Exception e) {
    if (e instanceof uz.hesap.service.common.exception.ApiException) {
      return ErrorCode.API_ERROR_CODE.value;
    }
    if (e instanceof ExceptionInterface ei) {
      return ei.getCode().value;
    }
    if (e instanceof java.util.NoSuchElementException) {
      return ErrorCode.NOT_FOUND_ERROR_CODE.value;
    }
    if (e instanceof NullPointerException) {
      return ErrorCode.NULL_POINTER_ERROR_CODE.value;
    }
    if (e instanceof UnsupportedOperationException) {
      return ErrorCode.UNSUPPORTED_OPERATION_ERROR_CODE.value;
    }
    if (e instanceof IllegalArgumentException) {
      return ErrorCode.INVALID_ARGUMENT_ERROR_CODE.value;
    }
    return ErrorCode.INTERNAL_ERROR_CODE.value;
  }
}
