package uz.hesap.service.integration.service.amo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.AmoTokenEntity;
import uz.hesap.service.integration.model.amo.AmoInitRequest;
import uz.hesap.service.integration.model.amo.AmoTokenView;
import uz.hesap.service.integration.repository.AmoTokenRepository;

// amoCRM OAuth2 token boshqaruvi (eski crm-app AmoAuthService reaktiv varianti).
// Token DB'da (integration.amo_token) saqlanadi; getAccessToken oxirgi tokenni oladi,
// muddati o'tsa (server_time + expires_in) refresh_token bilan avtoyangilaydi.
@Log4j2
@Service
public class AmoAuthService {

  // Muddat tugashiga yaqin (60s) tokenni oldindan yangilaymiz.
  private static final long SAFETY_MARGIN_SEC = 60;

  private final WebClient.Builder webClientBuilder;
  private final AmoTokenRepository tokenRepository;
  private final ObjectMapper objectMapper;
  private final String oauthUrl;
  private final String clientId;
  private final String redirectUri;

  public AmoAuthService(
      WebClient.Builder webClientBuilder,
      AmoTokenRepository tokenRepository,
      ObjectMapper objectMapper,
      @Value("${application.amocrm.oauth-url}") String oauthUrl,
      @Value("${application.amocrm.client-id}") String clientId,
      @Value("${application.amocrm.redirect-uri}") String redirectUri) {
    this.webClientBuilder = webClientBuilder;
    this.tokenRepository = tokenRepository;
    this.objectMapper = objectMapper;
    this.oauthUrl = oauthUrl;
    this.clientId = clientId;
    this.redirectUri = redirectUri;
  }

  // Yaroqli access_token — muddati o'tgan bo'lsa avval yangilaydi.
  public Mono<String> accessToken() {
    return tokenRepository
        .findTopByOrderByCreatedDateDesc()
        .switchIfEmpty(
            Mono.error(
                new BadRequestException(
                    "amoCRM token yo'q. Avval /integration/v1/amo/auth/init orqali ulaning "
                        + "yoki eski tokenni ko'chiring.")))
        .flatMap(
            token -> {
              long now = Instant.now().getEpochSecond();
              long serverTime = token.getServerTime() == null ? 0 : token.getServerTime();
              long expiresIn = token.getExpiresIn() == null ? 0 : token.getExpiresIn();
              if (serverTime + expiresIn - SAFETY_MARGIN_SEC < now) {
                return refreshFrom(token).map(AmoTokenEntity::getAccessToken);
              }
              return Mono.just(token.getAccessToken());
            });
  }

  // Boshlang'ich ulanish (authorization_code) — clientSecret va code qo'lda beriladi.
  public Mono<AmoTokenView> initAuth(AmoInitRequest request) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("client_id", clientId);
    body.put("client_secret", request.clientSecret());
    body.put("grant_type", "authorization_code");
    body.put("redirect_uri", redirectUri);
    body.put("code", request.code());
    return authorizeAndSave(body, clientId, request.clientSecret(), redirectUri).map(this::toView);
  }

  // Oxirgi tokenning refresh_token'i bilan yangilash (cron / qo'lda).
  public Mono<AmoTokenView> updateToken() {
    return tokenRepository
        .findTopByOrderByCreatedDateDesc()
        .switchIfEmpty(Mono.error(new BadRequestException("amoCRM token yo'q")))
        .flatMap(this::refreshFrom)
        .map(this::toView);
  }

  // Joriy token holati (maxfiy qiymatlarsiz).
  public Mono<AmoTokenView> tokenView() {
    return tokenRepository
        .findTopByOrderByCreatedDateDesc()
        .map(this::toView)
        .defaultIfEmpty(new AmoTokenView(false, null, null, null));
  }

  private Mono<AmoTokenEntity> refreshFrom(AmoTokenEntity token) {
    String cId = token.getClientId() != null ? token.getClientId() : clientId;
    String redirect = token.getRedirectUri() != null ? token.getRedirectUri() : redirectUri;
    ObjectNode body = objectMapper.createObjectNode();
    body.put("client_id", cId);
    body.put("client_secret", token.getClientSecret());
    body.put("grant_type", "refresh_token");
    body.put("redirect_uri", redirect);
    body.put("refresh_token", token.getRefreshToken());
    return authorizeAndSave(body, cId, token.getClientSecret(), redirect);
  }

  private Mono<AmoTokenEntity> authorizeAndSave(
      ObjectNode body, String cId, String clientSecret, String redirect) {
    return webClientBuilder
        .build()
        .post()
        .uri(oauthUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        b -> {
                          log.warn("amoCRM auth failed: {} {}", resp.statusCode(), b);
                          return Mono.error(
                              new BadRequestException("amoCRM auth failed: " + resp.statusCode()));
                        }))
        .bodyToMono(String.class)
        .flatMap(json -> parseAndBuild(json, cId, clientSecret, redirect))
        .flatMap(tokenRepository::save);
  }

  private Mono<AmoTokenEntity> parseAndBuild(
      String json, String cId, String clientSecret, String redirect) {
    try {
      JsonNode node = objectMapper.readTree(json);
      String access = node.path("access_token").asText(null);
      if (access == null || access.isBlank()) {
        return Mono.error(new BadRequestException("amoCRM javobida access_token yo'q"));
      }
      AmoTokenEntity entity = new AmoTokenEntity();
      entity.setClientId(cId);
      entity.setClientSecret(clientSecret);
      entity.setRedirectUri(redirect);
      entity.setAccessToken(access);
      entity.setRefreshToken(node.path("refresh_token").asText(null));
      entity.setExpiresIn(node.path("expires_in").asLong(0));
      entity.setServerTime(node.path("server_time").asLong(Instant.now().getEpochSecond()));
      entity.setTokenType(node.path("token_type").asText(null));
      return Mono.just(entity);
    } catch (Exception e) {
      return Mono.error(new BadRequestException("amoCRM token javobini o'qib bo'lmadi"));
    }
  }

  private AmoTokenView toView(AmoTokenEntity t) {
    return new AmoTokenView(true, t.getExpiresIn(), t.getServerTime(), t.getCreatedDate());
  }
}
