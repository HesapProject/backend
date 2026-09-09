package uz.hesap.service.main.context;

import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.UnauthorizedException;

@Log4j2
@Component
@Order(-2)
public class CustomErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

  public CustomErrorWebExceptionHandler(
      ErrorAttributes errorAttributes,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer) {
    super(errorAttributes, new WebProperties.Resources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    Map<String, Object> errorProperties =
        getErrorAttributes(request, ErrorAttributeOptions.defaults());
    Throwable error = getError(request);
    final String path = request.uri().getPath();

    log.error("Failed to request [{}] path. Error:", path, error);

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    if (error instanceof UnauthorizedException) {
      errorProperties.put("error", "Unauthorized");
      errorProperties.put("status", 401);
      status = HttpStatus.UNAUTHORIZED;
    }

    if (error instanceof IllegalArgumentException) {
      errorProperties.put("error", "Bad Request");
      errorProperties.put("status", 400);
      status = HttpStatus.BAD_REQUEST;
    }

    return ServerResponse.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(errorProperties));
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(
      final ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }
}
