package uz.hesap.service.common.exception.handler;

import static org.springframework.http.HttpStatus.*;

import java.io.IOException;
import java.net.ConnectException;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import uz.hesap.service.common.exception.*;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("uz");

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler({Exception.class, RuntimeException.class})
  public final ResponseEntity<?> handleException(
      final Exception e, final ServerWebExchange exchange) {
    return constructExceptionResponse(
        e, exchange, INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR_CODE);
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(IOException.class)
  public ResponseEntity<?> handleIOException(
      final IOException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, INTERNAL_SERVER_ERROR, ErrorCode.IO_EXCEPTION);
  }

  // ======== ConnectException — boshqa servisga ulanib bo'lmaganda ========
  @ResponseStatus(SERVICE_UNAVAILABLE)
  @ExceptionHandler(ConnectException.class)
  public ResponseEntity<?> handleConnectException(
      final ConnectException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(
        e, exchange, SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE_ERROR_CODE);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({InvalidOperationException.class})
  public ResponseEntity<?> handleBadRequests(
      final InvalidOperationException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(
        e, exchange, BAD_REQUEST, ErrorCode.INVALID_OPERATION_ERROR_CODE);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({InvalidArgumentException.class})
  public ResponseEntity<?> handleBadRequests(
      final InvalidArgumentException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, e.getCode());
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({MethodArgumentTypeMismatchException.class})
  public ResponseEntity<?> handleBadRequests(
      final MethodArgumentTypeMismatchException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, ErrorCode.INVALID_TYPE);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({ApiException.class})
  public ResponseEntity<?> handleBadRequests(
      final ApiException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, ErrorCode.API_ERROR_CODE);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({
    DateTimeParseException.class,
    UnsupportedOperationException.class,
    IllegalArgumentException.class,
    IllegalStateException.class,
    NullPointerException.class
  })
  public ResponseEntity<?> handleBadRequests(
      final RuntimeException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, ErrorCode.BAD_REQUEST_CODE);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({BadRequestException.class})
  public ResponseEntity<?> handleBadRequests(
      final BadRequestException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, e.getCode());
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({AlreadyExistsException.class})
  public ResponseEntity<?> handleBadRequests(
      final AlreadyExistsException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, e.getCode());
  }

  @ResponseStatus(FORBIDDEN)
  @ExceptionHandler({
    ForbiddenException.class,
    ListForbiddenException.class,
    RetrieveForbiddenException.class,
    UpdateForbiddenException.class,
    UserBlockedException.class,
  })
  public final ResponseEntity<?> handleForbiddenException(
      final ForbiddenException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, FORBIDDEN, e.getCode());
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class})
  public final ResponseEntity<?> handleException(
      final org.springframework.http.converter.HttpMessageNotReadableException e,
      final ServerWebExchange exchange) {
    if (e.getCause() != null
        && e.getCause().getCause() instanceof final InvalidArgumentException cause) {
      return constructExceptionResponse(e, exchange, BAD_REQUEST, cause.getCode());
    }
    return constructExceptionResponse(e, exchange, BAD_REQUEST, ErrorCode.REQUIRED_FIELD_MISSED);
  }

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler({NotFoundException.class})
  public ResponseEntity<?> handleNotFoundException(
      final NotFoundException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, NOT_FOUND, e.getCode());
  }

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler({NoSuchElementException.class})
  public ResponseEntity<?> handleNotFoundException(
      final NoSuchElementException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, NOT_FOUND, ErrorCode.NOT_FOUND_ERROR_CODE);
  }

  @ResponseStatus(UNAUTHORIZED)
  @ExceptionHandler({
    InvalidCredentialsException.class,
    InvalidTokenException.class,
    UnauthorizedException.class,
  })
  public final ResponseEntity<?> handleUnauthorizedException(
      final UnauthorizedException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, UNAUTHORIZED, ErrorCode.UNAUTHORIZED_ERROR_CODE);
  }

  @ExceptionHandler({ServerWebInputException.class})
  public final ResponseEntity<?> handleServerWebInputException(
      final ServerWebInputException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST, ErrorCode.REQUIRED_FIELD_MISSED);
  }

  @ResponseStatus(SERVICE_UNAVAILABLE)
  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<?> handleServiceUnavailableException(
      final ServiceUnavailableException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, SERVICE_UNAVAILABLE, e.getCode());
  }

  // ================ CORE — response builder ================

  protected ResponseEntity<ExceptionResponse> constructExceptionResponse(
      final Exception e,
      final ServerWebExchange exchange,
      final HttpStatus status,
      final ErrorCode errorCode) {
    final String path = exchange.getRequest().getPath().value();

    LOGGER.error("Failed to request [{}] path. Error:", path, e);

    // tilni headerdan olish: Accept-Language → uz, ru, eng
    Locale locale = resolveLocale(exchange);

    // i18n dan description olish — topilmasa exception message qaytadi
    String i18nDescription =
        messageSource.getMessage(errorCode.name(), null, e.getMessage(), locale);

    ExceptionResponse exceptionResponse =
        new ExceptionResponse(e, path, status, errorCode, i18nDescription);
    return ResponseEntity.status(status).body(exceptionResponse);
  }

  /** Accept-Language headerdan tilni aniqlash. Default: uz */
  private Locale resolveLocale(ServerWebExchange exchange) {
    try {
      var acceptLanguages = exchange.getRequest().getHeaders().getAcceptLanguage();
      if (!acceptLanguages.isEmpty()) {
        String lang = acceptLanguages.getFirst().getRange();
        // "eng" → "en" mapping (properties fayli _eng deb nomlangan)
        if ("en".equalsIgnoreCase(lang)) {
          return Locale.forLanguageTag("eng");
        }
        return Locale.forLanguageTag(lang);
      }
    } catch (Exception ignored) {
      // header parsing xatolik bo'lsa — default
    }
    return DEFAULT_LOCALE;
  }
}
