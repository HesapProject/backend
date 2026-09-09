package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.myid.MyIdCodeRequest;
import uz.hesap.service.integration.model.myid.MyIdCreateSessionRequest;
import uz.hesap.service.integration.model.myid.MyIdCreateSessionResponse;
import uz.hesap.service.integration.model.myid.MyIdSdkConfigResponse;
import uz.hesap.service.integration.model.myid.MyIdUserDataResponse;
import uz.hesap.service.integration.service.MyIdService;

@RestController
@RequestMapping("/integration/v1/myid")
@RequiredArgsConstructor
public class MyIdController {

  private final MyIdService myIdService;

  @Value("${application.my-id.mobile-client-hash}")
  private String mobileClientHash;

  @Value("${application.my-id.mobile-client-hash-id}")
  private String mobileClientHashId;

  // Mobil ilova MyID SDK config — iOS hardcode o'rniga shu endpoint'dan oladi
  // (backend mobil client bilan har doim mos). Auth boshqa myid endpoint'lardek.
  @GetMapping("/sdk-config")
  public Mono<MyIdSdkConfigResponse> sdkConfig() {
    return Mono.just(
        new MyIdSdkConfigResponse(mobileClientHash, mobileClientHashId, "PRODUCTION"));
  }

  @PostMapping("/session")
  public Mono<MyIdCreateSessionResponse> getCompanies(
      @RequestBody MyIdCreateSessionRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return myIdService.createSession(
        request,
        userPrincipal.user().companyId(),
        userPrincipal.user().id(),
        request.platform());
  }

  @PostMapping("/code")
  public Mono<MyIdUserDataResponse> sendCode(
      @RequestBody MyIdCodeRequest request, @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return myIdService.getUserData(
        request.code(),
        userPrincipal.user().companyId(),
        userPrincipal.user().id(),
        userPrincipal.user().identifier(),
        request.platform());
  }

  // Web SDK imzolash URL — backend session yaratib web.myid.uz URL'ini qaytaradi.
  // Frontend foydalanuvchini shu URL'ga yo'naltiradi (yuz tekshiruvi web.myid.uz
  // tarafida), auth_code redirectUrl'ga qaytadi. Imzolovchi ma'lumoti OneID
  // profilidan olinadi (principal userId).
  @GetMapping("/web-sign-url")
  public Mono<UrlResponse> webSignUrl(
      @RequestParam String redirectUrl,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      ServerHttpRequest request) {
    return myIdService
        .buildWebSignUrl(userPrincipal.user().id(), redirectUrl, clientIp(request))
        .map(UrlResponse::new);
  }

  // Haqiqiy mijoz IP — gateway/proxy orqasida X-Forwarded-For birinchi qiymati.
  private static String clientIp(ServerHttpRequest request) {
    String xff = request.getHeaders().getFirst("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String realIp = request.getHeaders().getFirst("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    var remote = request.getRemoteAddress();
    return remote != null && remote.getAddress() != null
        ? remote.getAddress().getHostAddress()
        : null;
  }

  // Web OAuth authorize URL — eski oqim (ishlatilmaydi, web SDK URL ishlatiladi).
  @GetMapping("/oauth-url")
  public Mono<UrlResponse> oauthUrl(
      @RequestParam String redirectUrl, @RequestParam(required = false) String state) {
    return myIdService.buildOAuthUrl(redirectUrl, state).map(UrlResponse::new);
  }

  public record UrlResponse(String url) {}
}
