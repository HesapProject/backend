// package uz.hesap.api.gateway.context;
//
// import static org.springframework.http.HttpStatus.UNAUTHORIZED;
// import static org.springframework.http.MediaType.APPLICATION_JSON;
// import static uz.hesap.api.gateway.exception.ExceptionResponse.UNAUTHORIZED_ERROR_CODE;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.io.IOException;
// import java.util.List;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.log4j.Log4j2;
// import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// import org.springframework.cloud.gateway.filter.GlobalFilter;
// import org.springframework.core.io.buffer.DataBuffer;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.server.reactive.ServerHttpRequest;
// import org.springframework.http.server.reactive.ServerHttpResponse;
// import org.springframework.stereotype.Component;
// import org.springframework.web.server.ServerWebExchange;
// import reactor.core.publisher.Mono;
// import uz.hesap.api.gateway.exception.ExceptionResponse;
// import uz.hesap.api.gateway.model.UserInfo;
// import uz.hesap.api.gateway.service.UserService;
//
// @Log4j2
// @Component
// @RequiredArgsConstructor
// public class AuthenticationPreFilter implements GlobalFilter {
//
//  private static final String BEARER = "Bearer ";
//
//  private final ObjectMapper objectMapper;
//  private final UserService userService;
//
//  @Override
//  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
//    log.trace("Request arrived to authentication pre-filter");
//
//    final List<String> authHeaders =
//        exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
//    if (authHeaders == null || authHeaders.isEmpty()) {
//      log.error("Authorization header is missing in request");
//      return sendErrorMessage(exchange, "Authorization header is missing");
//    }
//
//    final String bearerToken = authHeaders.get(0);
//    if (!bearerToken.startsWith(BEARER)) {
//      log.error("Incomplete access token provided in request header");
//      return sendErrorMessage(exchange, "Incomplete access token");
//    }
//
//    return userService
//        .fetchUserInfo(bearerToken)
//        .onErrorResume(
//            e -> {
//              log.error("Failed to fetch user info. Error: ", e);
//
//              // Returning empty user down the chain and build proper error message
//              return Mono.just(UserInfo.EMPTY);
//            })
//        .flatMap(
//            userInfo -> {
//              if (userInfo.id() == null) {
//                return sendErrorMessage(exchange, "Access token has expired or invalid");
//              }
//
//              if (userInfo.emailVerified()) {
//                log.debug("Access token in request is valid");
//
//                return chain.filter(exchange);
//              } else {
//                log.debug("Invalid access token provided");
//
//                return sendErrorMessage(exchange, "Invalid access token");
//              }
//            });
//  }
//
//  private Mono<Void> sendErrorMessage(final ServerWebExchange exchange, String errorMsg) {
//    final ServerHttpRequest request = exchange.getRequest();
//    final ServerHttpResponse response = exchange.getResponse();
//
//    try {
//      final DataBuffer buffer =
//          response
//              .bufferFactory()
//              .wrap(
//                  objectMapper.writeValueAsBytes(
//                      new ExceptionResponse(
//                          UNAUTHORIZED_ERROR_CODE,
//                          errorMsg,
//                          request.getPath().value(),
//                          UNAUTHORIZED)));
//
//      response.getHeaders().setContentType(APPLICATION_JSON);
//      response.setStatusCode(UNAUTHORIZED);
//      return response.writeWith(Mono.just(buffer));
//    } catch (final IOException e) {
//      return Mono.error(new InternalError("Failed to convert POJO into JSON", e));
//    }
//  }
// }
