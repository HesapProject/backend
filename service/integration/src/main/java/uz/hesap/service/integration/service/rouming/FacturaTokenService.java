package uz.hesap.service.integration.service.rouming;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.RoumingSettingEntity;
import uz.hesap.service.integration.service.RoumingSettingService;

// Factura.uz OAuth2 (password grant) token oluvchi + keshlovchi.
// POST {tokenUrl} (form-urlencoded): grant_type=password + username + password +
// client_id + client_secret -> {access_token, expires_in}. Token expires_in gacha
// keshlanadi (60s xavfsizlik marjasi bilan). Bir vaqtda bir nechta so'rov kelsa
// bitta token qayta ishlatiladi.
@Log4j2
@Service
public class FacturaTokenService {

  private static final String DEFAULT_TOKEN_URL = "https://account.faktura.uz/token";
  private static final Duration SAFETY_MARGIN = Duration.ofSeconds(60);

  private final WebClient.Builder webClientBuilder;
  private final RoumingSettingService settingService;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  // Keshlangan token + amal qilish muddati.
  private final AtomicReference<CachedToken> cache = new AtomicReference<>();

  public FacturaTokenService(
      WebClient.Builder webClientBuilder,
      RoumingSettingService settingService,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.webClientBuilder = webClientBuilder;
    this.settingService = settingService;
    this.objectMapper = objectMapper;
  }

  // Yaroqli access_token qaytaradi (keshdan yoki yangisini olib).
  public Mono<String> accessToken() {
    CachedToken cached = cache.get();
    if (cached != null && cached.isValid()) {
      return Mono.just(cached.token());
    }
    return settingService.getCurrent().flatMap(this::fetchToken);
  }

  // Kesh bekor qilish (401 kelganda chaqiruvchi qayta urinishдан oldin).
  public void invalidate() {
    cache.set(null);
  }

  private Mono<String> fetchToken(RoumingSettingEntity s) {
    if (!notBlank(s.getLogin())
        || !notBlank(s.getPassword())
        || !notBlank(s.getClientId())
        || !notBlank(s.getClientSecret())) {
      return Mono.error(
          new BadRequestException(
              "Factura.uz credentials to'liq emas (username/parol/clientId/clientSecret). "
                  + "Control -> Integratsiyalar -> Rouming."));
    }
    String tokenUrl = notBlank(s.getTokenUrl()) ? s.getTokenUrl() : DEFAULT_TOKEN_URL;

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("username", s.getLogin());
    form.add("password", s.getPassword());
    form.add("client_id", s.getClientId());
    form.add("client_secret", s.getClientSecret());

    return webClientBuilder
        .build()
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromFormData(form))
        .retrieve()
        .onStatus(
            status -> status.isError(),
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        b -> {
                          log.warn("Factura.uz token failed: {} {}", resp.statusCode(), b);
                          return Mono.error(
                              new BadRequestException(
                                  "Factura.uz token failed: " + resp.statusCode()));
                        }))
        .bodyToMono(String.class)
        .map(this::parseAndCache);
  }

  // {access_token, expires_in} javobidan token olib keshlaydi.
  private String parseAndCache(String json) {
    try {
      var node = objectMapper.readTree(json);
      String token = node.path("access_token").asText(null);
      if (token == null || token.isBlank()) {
        throw new BadRequestException("Factura.uz token javobida access_token yo'q");
      }
      long expiresIn = node.path("expires_in").asLong(3600);
      Instant expiry = Instant.now().plusSeconds(expiresIn).minus(SAFETY_MARGIN);
      cache.set(new CachedToken(token, expiry));
      log.info("Factura.uz token olindi, {}s amal qiladi", expiresIn);
      return token;
    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new BadRequestException("Factura.uz token javobini o'qib bo'lmadi: " + e.getMessage());
    }
  }

  private static boolean notBlank(String v) {
    return v != null && !v.isBlank();
  }

  private record CachedToken(String token, Instant expiry) {
    boolean isValid() {
      return token != null && Instant.now().isBefore(expiry);
    }
  }
}
