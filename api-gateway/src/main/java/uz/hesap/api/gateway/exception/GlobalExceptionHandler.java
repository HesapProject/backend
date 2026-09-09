package uz.hesap.api.gateway.exception;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uz.hesap.api.gateway.exception.ExceptionResponse.UNAUTHORIZED_ERROR_CODE;

import java.net.ConnectException;
import java.util.NoSuchElementException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ServerWebExchange;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {

  @ResponseStatus(FORBIDDEN)
  @ExceptionHandler(ForbiddenException.class)
  public final ResponseEntity<ExceptionResponse> handleForbiddenException(
      final ForbiddenException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, FORBIDDEN);
  }

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler({ConnectException.class, NoSuchElementException.class})
  public ResponseEntity<ExceptionResponse> handleNotFoundException(
      final Exception e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, NOT_FOUND);
  }

  @ResponseStatus(UNAUTHORIZED)
  @ExceptionHandler(UnauthorizedException.class)
  public final ResponseEntity<ExceptionResponse> handleUnauthorizedException(
      final UnauthorizedException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, UNAUTHORIZED);
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(IllegalStateException.class)
  public final ResponseEntity<ExceptionResponse> handleUnauthorizedException(
      final IllegalStateException e, final ServerWebExchange exchange) {
    return constructExceptionResponse(e, exchange, BAD_REQUEST);
  }

  protected ResponseEntity<ExceptionResponse> constructExceptionResponse(
      final Exception e, final ServerWebExchange exchange, final HttpStatus status) {
    final String path = exchange.getRequest().getPath().value();

    log.error("Failed to request [{}] path. Error:", path, e);

    return new ResponseEntity<>(
        new ExceptionResponseBuilder()
            .exception(e)
            .code(UNAUTHORIZED_ERROR_CODE)
            .path(path)
            .status(status)
            .build(),
        status);
  }
}
