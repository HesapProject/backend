package uz.hesap.service.main.feign;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.main.model.response.BackendAuthResponse;
import uz.hesap.service.main.model.response.OneIdPassportResponse;
import uz.hesap.service.main.model.response.OneIdUserResponse;

// Integration servisining ichki /local/* endpoint'lariga kirish.
// sso.egov.uz bilan barcha aloqa integration tomonida — main-service faqat
// orchestrator (User/Device/Session/JWT).
@Log4j2
@Service
public class IntegrationServiceClient {

  // OneID profil saqlash so'rovi.
  public record OneIdProfileSaveRequest(UUID userId, OneIdUserResponse data) {}

  // OneID code → user data ayirboshlash.
  public record OneIdExchangeRequest(String code, String redirectUri) {}

  // SSO URL javobi.
  public record UrlResponse(String url) {}

  // Turanix MSISDN tekshirish so'rovi (integration TuranixCheckRequest'ga mos).
  public record TuranixCheckMsisdnRequest(UUID userId, String msisdn, String pinfl) {}

  // Turanix javobining kerakli qismi (resultCode==3000 → biriktirilgan).
  public record TuranixCheckResult(Integer resultCode, String description, String errorMessage) {}

  private final WebClient webClient;

  public IntegrationServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.integration-service.base-url:}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public Mono<String> getOneIdLoginUrl(String redirectUrl) {
    return getOneIdLoginUrl(redirectUrl, null, null, null);
  }

  // Scope/state/authMethods override'lari — CompanyService.getUrl uchun:
  // PERSONAL: scope=legal_info, state=customName; LEGAL: +auth_methods=LEPKCSMETHOD.
  public Mono<String> getOneIdLoginUrl(
      String redirectUrl, String scope, String state, String authMethods) {
    return webClient
        .get()
        .uri(
            uri -> {
              var b = uri.path("/integration/v1/local/oneid/url").queryParam("redirectUrl", redirectUrl);
              if (scope != null) b.queryParam("scope", scope);
              if (state != null) b.queryParam("state", state);
              if (authMethods != null) b.queryParam("authMethods", authMethods);
              return b.build();
            })
        .retrieve()
        .bodyToMono(UrlResponse.class)
        .map(UrlResponse::url);
  }

  // Turanix: MSISDN shu PINFL'ga biriktirilganini tekshirish. resultCode==3000 → ha.
  public Mono<TuranixCheckResult> checkPassMsisdn(UUID userId, String msisdn, String pinfl) {
    return webClient
        .post()
        .uri("/integration/v1/local/turanix/check-msisdn")
        .bodyValue(new TuranixCheckMsisdnRequest(userId, msisdn, pinfl))
        .retrieve()
        .bodyToMono(TuranixCheckResult.class);
  }

  // sso.egov.uz bilan code → user data ayirboshlash. Integration servisida.
  public Mono<OneIdUserResponse> exchangeOneIdCode(String code, String redirectUri) {
    return webClient
        .post()
        .uri("/integration/v1/local/oneid/exchange")
        .bodyValue(new OneIdExchangeRequest(code, redirectUri))
        .retrieve()
        .bodyToMono(OneIdUserResponse.class);
  }

  public Mono<Void> saveOneIdProfile(UUID userId, OneIdUserResponse data) {
    return webClient
        .post()
        .uri("/integration/v1/local/oneid/profile")
        .bodyValue(new OneIdProfileSaveRequest(userId, data))
        .retrieve()
        .bodyToMono(Void.class);
  }

  public Mono<OneIdPassportResponse> getOneIdPassport(UUID userId) {
    return webClient
        .get()
        .uri("/integration/v1/local/oneid/passport/{userId}", userId)
        .retrieve()
        .bodyToMono(OneIdPassportResponse.class);
  }

  // E-IMZO: /backend/auth — sertifikat ma'lumotini olish (PINFL'ni ajratish uchun).
  public record EImzoAuthRequest(String pkcs7, String ipAddress) {}

  public Mono<BackendAuthResponse> eImzoAuth(String pkcs7, String ipAddress) {
    return webClient
        .post()
        .uri("/integration/v1/local/eimzo/auth")
        .bodyValue(new EImzoAuthRequest(pkcs7, ipAddress))
        .retrieve()
        .bodyToMono(BackendAuthResponse.class);
  }

  // MyID (web OAuth) code → imzolovchi pinfl. platform "WEB" — web OAuth credential.
  public record MyIdVerifyRequest(String code, UUID userId, String platform) {}

  public record MyIdVerifyResponse(
      String pinfl,
      Double comparisonValue,
      String firstName,
      String lastName,
      String middleName,
      String passport,
      String issuedBy,
      String issueDate,
      String expiryDate,
      String birthDate,
      String birthPlace,
      String nationality,
      String citizenship,
      String address) {}

  // MyID code'ni tekshirib pinfl qaytaradi (profilni verified qilish uchun).
  // platform: WEB → web OAuth, MOBILE → mobil SDK (/sdk/data) oqimi.
  public Mono<MyIdVerifyResponse> verifyMyId(String code, UUID userId, String platform) {
    return webClient
        .post()
        .uri("/integration/v1/local/myid/verify")
        .bodyValue(new MyIdVerifyRequest(code, userId, platform))
        .retrieve()
        .bodyToMono(MyIdVerifyResponse.class);
  }

  // AbleID attempt tekshiruvi: sessiya kimga ochilgan (pinfl) + status.
  public record AbleIdVerifyRequest(String attemptId) {}

  public record AbleIdVerifyResponse(String pinfl, String status) {}

  public Mono<AbleIdVerifyResponse> verifyAbleId(String attemptId) {
    return webClient
        .post()
        .uri("/integration/v1/local/able-id/verify")
        .bodyValue(new AbleIdVerifyRequest(attemptId))
        .retrieve()
        .bodyToMono(AbleIdVerifyResponse.class);
  }

  // Balansdan yechish (paket xaridi) — SUMMA balansidan − qiladi va − transaction yozadi.
  // Balans yetmasa integration 400 qaytaradi → xarid bekor bo'ladi.
  public record BalanceWithdrawRequest(
      UUID uniqueId, Double amount, String type, String description) {}

  public Mono<Void> withdrawBalance(UUID uniqueId, Double amount, String type, String description) {
    return webClient
        .post()
        .uri("/integration/v1/local/balance/withdraw")
        .bodyValue(new BalanceWithdrawRequest(uniqueId, amount, type, description))
        .retrieve()
        // Integration 4xx (masalan "Balansda mablag' yetarli emas") — mazmunli xato
        // sifatida uzatamiz, aks holda WebClientResponseException opaque 500 bo'lardi.
        .onStatus(
            HttpStatusCode::is4xxClientError,
            resp ->
                resp.bodyToMono(java.util.Map.class)
                    .defaultIfEmpty(java.util.Map.of())
                    .flatMap(
                        body -> {
                          Object msg = body.get("message");
                          return Mono.error(
                              new BadRequestException(
                                  msg != null
                                      ? msg.toString()
                                      : "Balans amalini bajarib bo'lmadi"));
                        }))
        .bodyToMono(Void.class);
  }

  public record PromoCheckResult(boolean valid, String discountType, Double discountAmount) {}

  // Promokod tekshirish (integration'dagi promos). Javobni flatten qilamiz.
  public Mono<PromoCheckResult> checkPromo(String code) {
    return webClient
        .get()
        .uri("/integration/v1/local/promos/check/{code}", code)
        .retrieve()
        .bodyToMono(java.util.Map.class)
        .map(
            m -> {
              Object valid = m.get("valid");
              Object promo = m.get("promo");
              String dt = null;
              Double da = null;
              if (promo instanceof java.util.Map<?, ?> pm) {
                Object t = pm.get("discountType");
                dt = t != null ? t.toString() : null;
                Object a = pm.get("discountAmount");
                da = a instanceof Number n ? n.doubleValue() : null;
              }
              return new PromoCheckResult(Boolean.TRUE.equals(valid), dt, da);
            })
        .onErrorResume(e -> Mono.just(new PromoCheckResult(false, null, null)));
  }

  public record PurchaseRecordRequest(
      String userIn,
      Double amount,
      String promo,
      uz.hesap.service.common.util.enums.PaymentMethod method,
      String unitType,
      UUID unitId,
      UUID createdBy) {}

  public Mono<Void> recordPurchase(
      String userIn,
      Double amount,
      String promo,
      uz.hesap.service.common.util.enums.PaymentMethod method,
      String unitType,
      UUID unitId,
      UUID createdBy) {
    return webClient
        .post()
        .uri("/integration/v1/local/purchases")
        .bodyValue(
            new PurchaseRecordRequest(userIn, amount, promo, method, unitType, unitId, createdBy))
        .retrieve()
        .bodyToMono(Void.class);
  }

  // To'g'ridan-to'g'ri paket xaridi: integration'da PENDING order yaratib, Payme/Click
  // checkout linkini oladi. amount so'mda (narx main'da server tomonda hisoblanadi).
  public record PaymentOrderRequest(
      UUID uniqueId, String userIn, UUID packageId, Integer amount, String promoCode, String provider) {}

  public Mono<UrlResponse> createPaymentOrder(
      UUID uniqueId, String userIn, UUID packageId, Integer amount, String promoCode, String provider) {
    return webClient
        .post()
        .uri("/integration/v1/local/payment-orders")
        .bodyValue(new PaymentOrderRequest(uniqueId, userIn, packageId, amount, promoCode, provider))
        .retrieve()
        .bodyToMono(UrlResponse.class);
  }
}
