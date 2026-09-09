package uz.hesap.service.log.context;

import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;

public class JwtConverter implements Function<ServerWebExchange, Mono<Authentication>> {

  private final WebClient webClient;

  public JwtConverter(final WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public Mono<Authentication> apply(ServerWebExchange exchange) {
    return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
        .filter(authHeader -> authHeader.startsWith("Bearer "))
        .flatMap(this::convert);
  }

  private Mono<Authentication> convert(String authToken) {

    return webClient
        .get()
        .uri("/main/v1/users/me")
        .header("Authorization", authToken)
        .retrieve()
        .bodyToMono(UserResponse.class)
        .map(
            user -> {
              // Admin token uchun device null bo'lishi mumkin.
              var sessionId = user.device() != null ? user.device().id() : null;
              return new CurrentUserAuthenticationToken(
                  new UserPrincipal(user, authToken, sessionId));
            });
  }
}
