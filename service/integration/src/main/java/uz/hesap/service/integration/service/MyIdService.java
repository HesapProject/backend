package uz.hesap.service.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.message.MyIdLogReply;
import uz.hesap.service.integration.domain.MyIdProfileEntity;
import uz.hesap.service.integration.domain.MyIdSettingEntity;
import uz.hesap.service.integration.model.mapper.MyIdMapper;
import uz.hesap.service.integration.model.myid.MyIdClientAuthRequest;
import uz.hesap.service.integration.model.myid.MyIdCreateSessionRequest;
import uz.hesap.service.integration.model.myid.MyIdCreateSessionResponse;
import uz.hesap.service.integration.model.myid.MyIdErrorResponse;
import uz.hesap.service.integration.model.myid.MyIdOAuthMeResponse;
import uz.hesap.service.integration.model.myid.MyIdPlatform;
import uz.hesap.service.integration.model.myid.MyIdTokenResponse;
import uz.hesap.service.integration.model.myid.MyIdWebSessionRequest;
import uz.hesap.service.integration.model.myid.MyIdUserDataResponse;
import uz.hesap.service.integration.repository.MyIdProfileRepository;
import uz.hesap.service.integration.repository.OneIdUserRepository;
import uz.hesap.service.jms.JmsPublisher;

@Service
@Log4j2
public class MyIdService {

  // Har platforma uchun alohida OAuth client token kesh kaliti.
  private static String tokenKey(MyIdPlatform platform) {
    return "MY_ID_TOKEN_" + platform.name();
  }

  // Web SDK client_credentials token kesh kaliti (web sessions uchun).
  private static final String WEB_CC_TOKEN = "MY_ID_WEB_CC_TOKEN";

  // baseUrl/clientId/clientSecret runtime'da DB'dan (yoki yml fallback'dan)
  // o'qiladi — shu sababli WebClient baseUrl bilan bog'lanmagan, har request
  // absolute URI ishlatadi.
  private final WebClient webClient;
  private final Cache<String, String> cachedMyIdToken;
  private final JmsPublisher jmsPublisher;
  private final MyIdProfileRepository myIdProfileRepository;
  private final MyIdMapper myIdMapper;
  private final ObjectMapper objectMapper;
  private final MyIdSettingService myIdSettingService;
  // OneID profil — web imzolash uchun imzolovchining pinfl/birthDate/phone'i.
  private final OneIdUserRepository oneIdUserRepository;

  // Web OAuth host SDK host'idan (api.myid.uz) farq qiladi — OAuth endpointlari
  // myid.uz da (api.myid.uz/.../oauth2/... → 404). Env bo'yicha sozlanadi.
  private final String oauthBaseUrl;
  // MyID Web SDK hosti (yuz tekshiruvi sahifasi). Prod: web.myid.uz, dev: web.devmyid.uz.
  private final String webBaseUrl;

  public MyIdService(
      WebClient.Builder builder,
      @Qualifier("cachedMyIdToken") Cache<String, String> cachedMyIdToken,
      JmsPublisher jmsPublisher,
      MyIdProfileRepository myIdProfileRepository,
      MyIdMapper myIdMapper,
      ObjectMapper objectMapper,
      MyIdSettingService myIdSettingService,
      OneIdUserRepository oneIdUserRepository,
      @Value("${application.my-id.oauth-base-url:https://myid.uz}") String oauthBaseUrl,
      @Value("${application.my-id.web-base-url:https://web.myid.uz}") String webBaseUrl) {
    this.webClient = builder.build();
    this.cachedMyIdToken = cachedMyIdToken;
    this.jmsPublisher = jmsPublisher;
    this.myIdProfileRepository = myIdProfileRepository;
    this.myIdMapper = myIdMapper;
    this.objectMapper = objectMapper;
    this.myIdSettingService = myIdSettingService;
    this.oneIdUserRepository = oneIdUserRepository;
    this.oauthBaseUrl = oauthBaseUrl;
    this.webBaseUrl = webBaseUrl;
  }

  // Web SDK imzolash URL'ini quradi: web SDK session yaratadi (web.myid.uz
  // oqimi) va web.myid.uz URL'ini qaytaradi. pinfl/birth_date imzolovchi OneID
  // profilidan URL'ga qo'shiladi. Foydalanuvchi yuz tekshiruvidan o'tib,
  // redirect_uri'ga auth_code bilan qaytadi.
  public Mono<String> buildWebSignUrl(UUID userId, String redirectUrl, String ipAddress) {
    return oneIdUserRepository
        .findById(userId)
        .switchIfEmpty(
            Mono.error(
                new BadRequestException("OneID profil topilmadi — MyID imzolash uchun ma'lumot yo'q")))
        .flatMap(
            profile ->
                createWebSession(ipAddress)
                    .map(
                        sessionId ->
                            UriComponentsBuilder.fromUriString(webBaseUrl + "/")
                                .queryParam("session_id", sessionId)
                                .queryParam("pinfl", profile.getPin())
                                .queryParam("birth_date", profile.getBirthDate())
                                .queryParam("redirect_uri", redirectUrl)
                                .queryParam("lang", "uz")
                                .build()
                                .toUriString()));
  }

  // Web SDK uchun client_credentials token (myid.uz/oauth2/access-token).
  // Mobil SDK token (/auth/clients/access-token)dan farq qiladi. Keshlanadi.
  private Mono<String> webClientCredentialsToken() {
    return Mono.justOrEmpty(cachedMyIdToken.getIfPresent(WEB_CC_TOKEN))
        .switchIfEmpty(
            myIdSettingService
                .getCurrent()
                .flatMap(
                    setting ->
                        webClient
                            .post()
                            .uri(oauthBaseUrl + "/v1/oauth2/access-token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(
                                org.springframework.web.reactive.function.BodyInserters.fromFormData(
                                        "grant_type", "client_credentials")
                                    .with("client_id", setting.clientId(MyIdPlatform.WEB))
                                    .with("client_secret", setting.clientSecret(MyIdPlatform.WEB)))
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, this::extractError)
                            .bodyToMono(MyIdTokenResponse.class)
                            .map(MyIdTokenResponse::accessToken)
                            .doOnNext(t -> cachedMyIdToken.put(WEB_CC_TOKEN, t))));
  }

  // Web SDK session: POST myid.uz/api/v1/web/sessions → session_id.
  private Mono<String> createWebSession(String ipAddress) {
    return webClientCredentialsToken()
        .flatMap(
            token ->
                webClient
                    .post()
                    .uri(oauthBaseUrl + "/v1/web/sessions")
                    .header("Authorization", bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new MyIdWebSessionRequest(3, UUID.randomUUID().toString(), ipAddress))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::extractError)
                    .bodyToMono(MyIdCreateSessionResponse.class)
                    .map(MyIdCreateSessionResponse::sessionId));
  }

  // SDK client OAuth token — platformaga mos credential bilan.
  public Mono<MyIdTokenResponse> authenticate(MyIdPlatform platform) {
    return myIdSettingService
        .getCurrent()
        .flatMap(
            setting ->
                webClient
                    .post()
                    .uri(absolute(setting, "/api/v1/auth/clients/access-token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        new MyIdClientAuthRequest(
                            setting.clientId(platform), setting.clientSecret(platform)))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::extractError)
                    .bodyToMono(MyIdTokenResponse.class))
        .doOnNext(r -> cachedMyIdToken.put(tokenKey(platform), r.accessToken()))
        .doOnError(e -> log.error("MyID authentication error, platform={}", platform, e));
  }

  // SDK session — platforma so'rovdan keladi (default MOBILE).
  public Mono<MyIdCreateSessionResponse> createSession(
      MyIdCreateSessionRequest request, UUID companyId, UUID userId, MyIdPlatform platform) {
    return createSessionWebClient(request, companyId, userId, platform);
  }

  public Mono<MyIdUserDataResponse> getUserData(
      String code, UUID companyId, UUID userId, String userIn, MyIdPlatform platform) {
    return Mono.zip(getClientAccessToken(platform), myIdSettingService.getCurrent())
        .flatMap(
            tuple -> {
              String token = tuple.getT1();
              MyIdSettingEntity setting = tuple.getT2();
              String url =
                  UriComponentsBuilder.fromUriString(setting.getBaseUrl() + "/api/v1/sdk/data")
                      .queryParam("code", code)
                      .toUriString();
              return webClient
                  .get()
                  .uri(url)
                  .header("Authorization", bearer(token))
                  .retrieve()
                  .onStatus(
                      HttpStatusCode::isError,
                      response ->
                          extractAndLogError(
                              companyId,
                              response,
                              userId,
                              "MyID user data retrieval failed",
                              "code=" + code))
                  .bodyToMono(MyIdUserDataResponse.class);
            })
        .flatMap(
            response ->
                sendSuccessLog(companyId, userId, "code=" + code, toJson(response))
                    .then(saveMyIdProfile(response, userIn))
                    .thenReturn(response))
        .doOnError(e -> log.error("MyID request error, userId={}", userId, e));
  }

  private Mono<MyIdCreateSessionResponse> createSessionWebClient(
      MyIdCreateSessionRequest body, UUID companyId, UUID userId, MyIdPlatform platform) {
    String requestBody = toJson(body);
    return Mono.zip(getClientAccessToken(platform), myIdSettingService.getCurrent())
        .flatMap(
            tuple -> {
              String token = tuple.getT1();
              MyIdSettingEntity setting = tuple.getT2();
              return webClient
                  .post()
                  .uri(absolute(setting, "/api/v2/sdk/sessions"))
                  .header("Authorization", bearer(token))
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(body)
                  .retrieve()
                  .onStatus(
                      HttpStatusCode::is4xxClientError,
                      response ->
                          extractAndLogError(
                              companyId,
                              response,
                              userId,
                              "MyID session creation failed",
                              requestBody))
                  .bodyToMono(MyIdCreateSessionResponse.class)
                  .flatMap(
                      r ->
                          sendSuccessLog(companyId, userId, requestBody, toJson(r)).thenReturn(r));
            });
  }

  private Mono<String> getClientAccessToken(MyIdPlatform platform) {
    return Mono.justOrEmpty(cachedMyIdToken.getIfPresent(tokenKey(platform)))
        .switchIfEmpty(authenticate(platform).map(MyIdTokenResponse::accessToken));
  }

  private Mono<MyIdProfileEntity> saveMyIdProfile(MyIdUserDataResponse response, String userIn) {
    MyIdProfileEntity entity = myIdMapper.toEntity(response);
    // userIn (JWT PINFL) bo'lmasa (s2s verify) — pasport PINFL'iga tushamiz.
    entity.setUserIn(userIn != null && !userIn.isBlank() ? userIn : entity.getPinfl());
    return myIdProfileRepository.save(entity);
  }

  private Mono<Void> sendSuccessLog(UUID companyId, UUID userId, String request, String response) {
    return sendLog(companyId, userId, "SUCCESS", null, request, response);
  }

  private Mono<Throwable> extractError(ClientResponse response) {
    // MyID xatoni text/plain bilan ham qaytaradi — JSON deb o'qisak
    // "content type text/plain not supported" bilan yiqilardi. Xom matn o'qiymiz.
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(raw -> new BadRequestException(myIdErrorMessage(raw, "MyID authentication failed")));
  }

  private Mono<Throwable> extractAndLogError(
      UUID companyId,
      ClientResponse response,
      UUID userId,
      String defaultMessage,
      String requestBody) {
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .flatMap(
            raw -> {
              String message = myIdErrorMessage(raw, defaultMessage);
              return sendLog(companyId, userId, "ERROR", message, requestBody, raw)
                  .then(Mono.error(new BadRequestException(message)));
            });
  }

  // MyID xato javobi JSON ham, text/plain ham bo'lishi mumkin. JSON bo'lsa
  // MyIdErrorResponse'dan xabarni olamiz, aks holda xom matnni ishlatamiz.
  private String myIdErrorMessage(String raw, String fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      MyIdErrorResponse err = objectMapper.readValue(raw, MyIdErrorResponse.class);
      String m = err.getMessage();
      if (m != null && !m.isBlank()) {
        return m;
      }
    } catch (final Exception ignored) {
      // JSON emas (text/plain) — xom matnni ishlatamiz.
    }
    return raw.length() > 500 ? raw.substring(0, 500) : raw;
  }

  private Mono<Void> sendLog(
      final UUID companyId,
      final UUID userId,
      final String status,
      final String errorMessage,
      final String request,
      final String response) {
    return jmsPublisher.publish(
        new MyIdLogReply(
            companyId, userId, status, errorMessage, request, response, Instant.now()));
  }

  // ======================== WEB OAuth oqimi (redirect) ========================

  // MyID OAuth authorize URL — frontend foydalanuvchini shu yerga yo'naltiradi,
  // yuz tekshiruvi myid.uz tarafida o'tadi, code redirect_uri'ga qaytadi.
  public Mono<String> buildOAuthUrl(String redirectUrl, String state) {
    return myIdSettingService
        .getCurrent()
        .map(
            setting ->
                UriComponentsBuilder.fromUriString(oauthBaseUrl + "/v1/oauth2/authorization")
                    // OAuth (web) oqimi — web credential juftligi.
                    .queryParam("client_id", setting.clientId(MyIdPlatform.WEB))
                    .queryParam("response_type", "code")
                    .queryParam("redirect_uri", redirectUrl)
                    .queryParam("scope", "common_data")
                    .queryParam("method", "strong")
                    .queryParam("state", state != null ? state : "myid_sign")
                    .build()
                    .toUriString());
  }

  // SDK code → access_token → /users/me → pinfl (imzolovchini aniqlash uchun).
  // Mobil ham, web ham natija code'ini /oauth2/access-token orqali almashtiradi;
  // faqat platformaga mos credential ishlatiladi.
  public Mono<String> oauthGetPinfl(String code, UUID userId, MyIdPlatform platform) {
    return myIdSettingService
        .getCurrent()
        .flatMap(
            setting ->
                webClient
                    .post()
                    .uri(oauthBaseUrl + "/v1/oauth2/access-token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                        org.springframework.web.reactive.function.BodyInserters.fromFormData(
                                "grant_type", "authorization_code")
                            .with("code", code)
                            .with("client_id", setting.clientId(platform))
                            .with("client_secret", setting.clientSecret(platform))
                            // MyID WebSDK docs: access-token so'roviga method+scope ham kerak,
                            // aks holda /users/me common_data (pinfl) qaytarmaydi.
                            .with("method", "strong")
                            .with("scope", "common_data"))
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::isError,
                        r ->
                            extractAndLogError(
                                null, r, userId, "MyID OAuth access-token xato", "code=" + code))
                    .bodyToMono(MyIdTokenResponse.class)
                    .flatMap(token -> oauthMe(token.accessToken(), userId)))
        // muvaffaqiyatli natijani log'ga yozamiz (SDK oqimidagi kabi)
        .flatMap(
            me -> {
              String pinfl =
                  me.profile() != null && me.profile().commonData() != null
                      ? me.profile().commonData().pinfl()
                      : null;
              // Reactor null emit qila olmaydi — bo'sh string qaytaramiz (chaqiruvchi tekshiradi).
              String result = pinfl != null ? pinfl : "";
              return sendSuccessLog(null, userId, "oauth code=" + code, toJson(me))
                  .thenReturn(result);
            });
  }

  private Mono<MyIdOAuthMeResponse> oauthMe(String accessToken, UUID userId) {
    return webClient
        .get()
        .uri(oauthBaseUrl + "/v1/users/me")
        .header("Authorization", bearer(accessToken))
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            r -> extractAndLogError(null, r, userId, "MyID OAuth users/me xato", "GET /users/me"))
        .bodyToMono(MyIdOAuthMeResponse.class);
  }

  // Settingdagi baseUrl bilan path'ni birlashtirib absolute URI hosil qiladi.
  private String absolute(MyIdSettingEntity setting, String path) {
    return setting.getBaseUrl() + path;
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("Error converting object to JSON", e);
      return "{}";
    }
  }
}
