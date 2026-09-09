package uz.hesap.service.integration.service.katm;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.model.katm.KatmAuthRequest;
import uz.hesap.service.integration.model.katm.KatmTokenResponse;
import uz.hesap.service.integration.service.KatmSettingService;

// KATM /auth/login orqali JWT access token oladi va xotirada keshlaydi.
// Token exp: 1 kun — biz 23 soat keshlaymiz (xavfsizlik chegirmasi bilan).
@Log4j2
@Service
public class KatmTokenService {

  // KATM token amal qilish muddati 1 kun; biroz oldinroq yangilaymiz.
  private static final Duration TOKEN_TTL = Duration.ofHours(23);

  private final KatmSettingService settingService;
  private final WebClient.Builder webClientBuilder;
  private final AtomicReference<CachedToken> cache = new AtomicReference<>();

  public KatmTokenService(
      KatmSettingService settingService, WebClient.Builder webClientBuilder) {
    this.settingService = settingService;
    this.webClientBuilder = webClientBuilder;
  }

  // Yaroqli token bo'lsa keshdan, aks holda qayta login qilib qaytaradi.
  public Mono<String> getAccessToken() {
    CachedToken cached = cache.get();
    if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
      return Mono.just(cached.token());
    }
    return login();
  }

  // 401 olganda chaqiriladi — keshni tozalab, yangi token oladi.
  public Mono<String> refresh() {
    cache.set(null);
    return login();
  }

  private Mono<String> login() {
    return settingService
        .getCurrent()
        .handle(
            (setting, sink) -> {
              if (isBlank(setting.getBaseUrl())
                  || isBlank(setting.getLogin())
                  || isBlank(setting.getPassword())) {
                sink.error(
                    new BadRequestException(
                        "KATM credentials not configured. "
                            + "POST /integration/v1/katm/settings orqali yarating."));
                return;
              }
              sink.next(setting);
            })
        .cast(uz.hesap.service.integration.domain.KatmSettingEntity.class)
        .flatMap(
            setting ->
                webClientBuilder
                    .baseUrl(setting.getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/auth/login")
                    .bodyValue(new KatmAuthRequest(setting.getLogin(), setting.getPassword()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::authError)
                    .bodyToMono(KatmTokenResponse.class))
        .flatMap(
            response -> {
              if (response.error() != null && response.error().errId() != null) {
                return Mono.error(new BadRequestException("KATM login: " + response.error().errMsg()));
              }
              if (response.data() == null || isBlank(response.data().accessToken())) {
                return Mono.error(new BadRequestException("KATM login: token kelmadi"));
              }
              String token = response.data().accessToken();
              cache.set(new CachedToken(token, Instant.now().plus(TOKEN_TTL)));
              log.info("KATM access token olindi");
              return Mono.just(token);
            });
  }

  private Mono<? extends Throwable> authError(ClientResponse clientResponse) {
    return clientResponse
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .flatMap(
            body -> {
              log.error("KATM auth error {}: {}", clientResponse.statusCode(), body);
              return Mono.error(
                  new BadRequestException(
                      "KATM login xatosi: " + clientResponse.statusCode().value()));
            });
  }

  private boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private record CachedToken(String token, Instant expiresAt) {}
}
