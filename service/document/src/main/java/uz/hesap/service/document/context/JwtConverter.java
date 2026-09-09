package uz.hesap.service.document.context;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.ApiKeyResolveResponse;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;

public class JwtConverter implements Function<ServerWebExchange, Mono<Authentication>> {

  private static final Logger LOGGER = LogManager.getLogger();

  // OpenAPI kaliti shu header'da keladi (Bearer JWT o'rniga).
  public static final String API_KEY_HEADER = "X-API-Key";
  // Kalit FAQAT public API yo'llarida ishlaydi — kabinet endpointlariga o'tkazilmaydi.
  private static final String OPEN_API_PATH_PREFIX = "/openapi/";

  private final WebClient webClient;

  // Token -> user keshi. Contract info kabi sahifalar bir vaqtda ko'p /document/
  // so'rov yuboradi; har biri /main/v1/users/me chaqirardi (kesh/retry yo'q edi) —
  // biror transient uzilishda o'sha so'rov 401 bo'lardi (masalan template 401,
  // contract esa 200). AsyncCache burst'ni bitta chaqiruvga birlashtiradi
  // (in-flight future ulashiladi); muvaffaqiyatsiz future keshlanmaydi (keyingi
  // so'rov qayta uriniladi). TTL qisqa — token bekor qilinsa ~60s ichida yangilanadi.
  private final AsyncCache<String, UserResponse> userCache =
      Caffeine.newBuilder()
          .expireAfterWrite(Duration.ofSeconds(60))
          .maximumSize(10_000)
          .buildAsync();

  // API kalit -> (ega + huquqlar) keshi. Bekor qilingan kalit ~30s ichida o'chadi.
  private final AsyncCache<String, ApiKeyResolveResponse> apiKeyCache =
      Caffeine.newBuilder()
          .expireAfterWrite(Duration.ofSeconds(30))
          .maximumSize(10_000)
          .buildAsync();

  public JwtConverter(final WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public Mono<Authentication> apply(ServerWebExchange exchange) {
    final String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
    if (apiKey != null && !apiKey.isBlank()) {
      return convertApiKey(apiKey.trim(), exchange.getRequest().getPath().value());
    }
    return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
        .filter(authHeader -> authHeader.startsWith("Bearer "))
        .flatMap(this::convert);
  }

  private Mono<Authentication> convert(String authToken) {
    return Mono.fromFuture(
            userCache.get(
                authToken,
                (token, executor) ->
                    webClient
                        .get()
                        .uri("/main/v1/users/me")
                        .header("Authorization", token)
                        .retrieve()
                        .bodyToMono(UserResponse.class)
                        .toFuture()))
        .map(
            user -> {
              // Admin token uchun device null bo'lishi mumkin.
              var sessionId = user.device() != null ? user.device().id() : null;
              return new CurrentUserAuthenticationToken(
                  new UserPrincipal(user, authToken, sessionId));
            });
  }

  // X-API-Key'ni main servisda tekshiradi va kalit egasi nomidan principal quradi.
  // Kalit kabinet endpointlariga o'tmasin — faqat /openapi/** yo'llari.
  private Mono<Authentication> convertApiKey(String apiKey, String path) {
    // Autentifikatsiya o'rnatilmasa Spring Security'ning entry point'i 401 qaytaradi —
    // reaktiv oqimda exception tashlashdan ko'ra shu ishonchli (500 bo'lib ketmaydi).
    if (!path.startsWith(OPEN_API_PATH_PREFIX)) {
      LOGGER.warn("API kalit /openapi/** dan tashqari yo'lda ishlatildi: {}", path);
      return Mono.empty();
    }
    return Mono.fromFuture(
            apiKeyCache.get(
                apiKey,
                (key, executor) ->
                    webClient
                        .post()
                        .uri("/main/v1/local/api-keys/resolve")
                        .bodyValue(new ApiKeyResolveBody(key))
                        .retrieve()
                        .bodyToMono(ApiKeyResolveResponse.class)
                        .toFuture()))
        .onErrorResume(
            e -> {
              LOGGER.warn("API kalit tekshiruvi muvaffaqiyatsiz: {}", e.getMessage());
              return Mono.empty();
            })
        .map(
            resolved ->
                new CurrentUserAuthenticationToken(
                    new UserPrincipal(resolved.owner(), null, null, resolved.apiKey())));
  }

  private record ApiKeyResolveBody(String key) {}
}
