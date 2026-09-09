package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.OneIdSettingEntity;
import uz.hesap.service.integration.model.oneid.OneIdUserResponse;
import uz.hesap.service.integration.model.oneid.TokenResponse;

// sso.egov.uz bilan barcha HTTP aloqa shu yerda — OAuth code → access_token →
// user data. Creds har request oldidan DB'dan o'qiladi (admin UI'da o'zgartirilgani
// darhol qo'llaniladi).
// NOTE: @RequiredArgsConstructor olib tashlandi — manual konstruktor WebClient.Builder
// ishlatadi. Ikkita konstruktor bo'lsa Spring ambiguous, default no-arg qidiradi →
// "No default constructor found" (EImzoHttpService bilan bir xil pattern).
@Log4j2
@Service
public class OneIdHttpService {

  private final OneIdSettingService settingService;
  private final WebClient webClient;

  public OneIdHttpService(
      OneIdSettingService settingService, WebClient.Builder webClientBuilder) {
    this.settingService = settingService;
    this.webClient = webClientBuilder.build();
  }

  // SSO login URL — frontend foydalanuvchini shu URL'ga yo'naltiradi.
  // Default: scope=user_info, state=client_auth (oddiy client login).
  // Override'lar bilan: COMPANY/PERSONAL flow (scope=legal_info, auth_methods=LEPKCSMETHOD).
  public Mono<String> buildLoginUrl(String redirectUrl) {
    return buildLoginUrl(redirectUrl, null, null, null);
  }

  public Mono<String> buildLoginUrl(
      String redirectUrl, String scope, String state, String authMethods) {
    return settingService
        .getCurrent()
        .map(
            c -> {
              UriComponentsBuilder b =
                  UriComponentsBuilder.fromUriString(
                          c.getBaseUrl() + "/sso/oauth/Authorization.do")
                      .queryParam("response_type", "one_code")
                      .queryParam("client_id", c.getClientId())
                      .queryParam("redirect_uri", redirectUrl)
                      .queryParam("scope", scope != null ? scope : "user_info")
                      .queryParam("state", state != null ? state : "client_auth");
              if (authMethods != null && !authMethods.isBlank()) {
                b.queryParam("auth_methods", authMethods);
              }
              return b.build().toUriString();
            });
  }

  // Code → access_token → user data — atomic ayirboshlash.
  public Mono<OneIdUserResponse> exchange(String code, String redirectUri) {
    return settingService
        .getCurrent()
        .flatMap(
            c -> accessToken(c, code, redirectUri).flatMap(token -> userData(c, token)));
  }

  private Mono<String> accessToken(OneIdSettingEntity c, String code, String redirectUri) {
    String url =
        UriComponentsBuilder.fromUriString(c.getBaseUrl() + "/sso/oauth/Authorization.do")
            .queryParam("grant_type", "one_authorization_code")
            .queryParam("client_id", c.getClientId())
            .queryParam("client_secret", c.getClientSecret())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("code", code)
            .toUriString();
    return webClient
        .post()
        .uri(url)
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .map(TokenResponse::access_token);
  }

  private Mono<OneIdUserResponse> userData(OneIdSettingEntity c, String accessToken) {
    String url =
        UriComponentsBuilder.fromUriString(c.getBaseUrl() + "/sso/oauth/Authorization.do")
            .queryParam("grant_type", "one_access_token_identify")
            .queryParam("client_id", c.getClientId())
            .queryParam("client_secret", c.getClientSecret())
            .queryParam("access_token", accessToken)
            .queryParam("scope", "user_info")
            .toUriString();
    return webClient.post().uri(url).retrieve().bodyToMono(OneIdUserResponse.class);
  }
}
