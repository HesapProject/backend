package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.FirebaseTopicReply;
import uz.hesap.service.integration.model.OneIdSettingInternal;
import uz.hesap.service.integration.model.SmsRequest;
import uz.hesap.service.integration.model.eimzo.EImzoAuthRequest;
import uz.hesap.service.integration.model.eimzo.EImzoAuthResponse;
import uz.hesap.service.integration.model.eimzo.EImzoVerifyRequest;
import uz.hesap.service.integration.model.eimzo.EImzoVerifyResponse;
import uz.hesap.service.integration.model.eskiz.EskizResponse;
import uz.hesap.service.integration.model.myid.MyIdCommonData;
import uz.hesap.service.integration.model.myid.MyIdPlatform;
import uz.hesap.service.integration.model.myid.MyIdVerifyRequest;
import uz.hesap.service.integration.model.myid.MyIdVerifyResponse;
import uz.hesap.service.integration.model.oneid.OneIdExchangeRequest;
import uz.hesap.service.integration.model.oneid.OneIdPassportResponse;
import uz.hesap.service.integration.model.oneid.OneIdProfileSaveRequest;
import uz.hesap.service.integration.model.oneid.OneIdUserResponse;
import uz.hesap.service.integration.model.turanix.TuranixCheckRequest;
import uz.hesap.service.integration.model.turanix.TuranixCheckResponse;
import uz.hesap.service.integration.service.EImzoHttpService;
import uz.hesap.service.integration.service.EskizProvider;
import uz.hesap.service.integration.service.FirebaseProvider;
import uz.hesap.service.integration.service.MyIdService;
import uz.hesap.service.integration.service.OneIdHttpService;
import uz.hesap.service.integration.service.OneIdProfileService;
import uz.hesap.service.integration.service.OneIdSettingService;
import uz.hesap.service.integration.service.turanix.TuranixService;

/**
 * Local Controller for internal service-to-service communication. Used by other services
 * (user, document) to send SMS via Eskiz, read OneID creds, and persist OneID profiles.
 *
 * <p>Path: /api/integration/v1/local/*
 */
@Log4j2
@RestController
@RequestMapping("/integration/v1/local")
@RequiredArgsConstructor
public class LocalController {

  private final EskizProvider smsService;
  private final FirebaseProvider firebaseProvider;
  private final OneIdSettingService oneIdSettingService;
  private final OneIdProfileService oneIdProfileService;
  private final OneIdHttpService oneIdHttpService;
  private final EImzoHttpService eImzoHttpService;
  private final MyIdService myIdService;
  private final uz.hesap.service.integration.service.AbleIdService ableIdService;
  private final TuranixService turanixService;
  private final uz.hesap.service.integration.service.rouming.RoumingService roumingService;
  private final uz.hesap.service.integration.service.payment.BalanceWithdrawService
      balanceWithdrawService;
  private final uz.hesap.service.integration.service.PurchaseService purchaseService;
  private final uz.hesap.service.integration.promos.PromosService promosService;
  private final uz.hesap.service.integration.service.payment.PackageOrderService
      packageOrderService;
  private final uz.hesap.service.integration.service.payment.PaymeService paymeService;
  private final uz.hesap.service.integration.service.payment.ClickService clickService;

  // To'g'ridan-to'g'ri paket xaridi uchun checkout order + Payme/Click link (s2s, main chaqiradi).
  // main narxni server tomonda hisoblab yuboradi (client summani buzolmaydi). PENDING order
  // saqlanadi; to'lov muvaffaqiyatli bo'lgach webhook uni topib paketni grant qiladi.
  @PostMapping("/payment-orders")
  public Mono<uz.hesap.service.integration.model.payme.CheckoutLinkModel> createPaymentOrder(
      @RequestBody PaymentOrderRequest request) {
    String provider = request.provider() == null ? "PAYME" : request.provider().toUpperCase();
    return packageOrderService
        .create(
            request.uniqueId(),
            request.userIn(),
            request.packageId(),
            request.amount(),
            request.promoCode(),
            provider)
        .flatMap(
            order ->
                "CLICK".equals(provider)
                    ? clickService.generateCheckoutLink(request.uniqueId(), request.amount())
                    : paymeService.generateCheckoutLink(request.uniqueId(), request.amount()));
  }

  public record PaymentOrderRequest(
      UUID uniqueId,
      String userIn,
      UUID packageId,
      Integer amount,
      String promoCode,
      String provider) {}

  // Promokod tekshirish (s2s, main chaqiradi — tokensiz). Public /promos/check auth talab qiladi.
  @GetMapping("/promos/check/{code}")
  public Mono<uz.hesap.service.integration.promos.PromosCheckResponse> checkPromoLocal(
      @PathVariable String code) {
    return promosService.check(code);
  }

  // Balansdan yechish (paket/tarif xaridi) — main chaqiradi. SUMMA balansidan − qiladi
  // va transaction'ga − qator yozadi. Yetmasa 400 qaytadi.
  @PostMapping("/balance/withdraw")
  @ResponseStatus(HttpStatus.OK)
  public Mono<Void> withdrawBalance(@RequestBody BalanceWithdrawRequest request) {
    uz.hesap.service.integration.domain.enums.TransactionType type;
    try {
      type =
          request.type() != null
              ? uz.hesap.service.integration.domain.enums.TransactionType.valueOf(request.type())
              : uz.hesap.service.integration.domain.enums.TransactionType.PACKAGE;
    } catch (IllegalArgumentException e) {
      type = uz.hesap.service.integration.domain.enums.TransactionType.WITHDRAWAL;
    }
    return balanceWithdrawService.withdraw(
        request.uniqueId(), request.amount(), type, request.description());
  }

  public record BalanceWithdrawRequest(
      java.util.UUID uniqueId, Double amount, String type, String description) {}

  @PostMapping("/sms/send")
  @ResponseStatus(HttpStatus.OK)
  public Mono<EskizResponse> sendSms(@RequestBody SmsRequest request) {
    log.info("Local SMS request received for phone: {}", request.phone());
    return smsService.send(request.phone(), request.message());
  }

  // FCM push — foydalanuvchiga (notification servis chaqiradi). Token resolve shu yerda.
  @PostMapping("/firebase/send-user")
  @ResponseStatus(HttpStatus.OK)
  public Mono<Void> sendFirebaseUser(@RequestBody FirebaseNotificationReply request) {
    return firebaseProvider.sendUser(request);
  }

  // FCM push — topic'ga (BLOG/ARTICLE). notification servis chaqiradi.
  @PostMapping("/firebase/send-topic")
  @ResponseStatus(HttpStatus.OK)
  public Mono<Void> sendFirebaseTopic(@RequestBody FirebaseTopicReply request) {
    return firebaseProvider.sendTopic(request);
  }

  // Turanix: MSISDN shu PINFL/pasportga biriktirilganini tekshirish (main-service uchun).
  @PostMapping("/turanix/check-msisdn")
  public Mono<TuranixCheckResponse> checkPassMsisdn(@RequestBody TuranixCheckRequest request) {
    return turanixService.checkPassMsisdn(request);
  }

  // Rouming: oldi-berdi tasdiqlanganda draft ЭСФ yaratish (document-service chaqiradi).
  // Xato asosiy oqimni buzmasin — chaqiruvchi onErrorResume bilan qamraydi.
  @PostMapping("/rouming/factura-draft")
  public Mono<String> createRoumingFacturaDraft(
      @RequestBody uz.hesap.service.integration.model.rouming.RoumingFacturaDraftRequest request) {
    return roumingService.createSellerFacturaDraft(request);
  }

  // OneID creds (secret bilan) — main-service uchun.
  @GetMapping("/oneid/setting")
  public Mono<OneIdSettingInternal> getOneIdSetting() {
    return oneIdSettingService
        .getCurrent()
        .map(e -> new OneIdSettingInternal(e.getBaseUrl(), e.getClientId(), e.getClientSecret()));
  }

  // OneID profil saqlash — main-service OneIdService chaqiradi.
  @PostMapping("/oneid/profile")
  @ResponseStatus(HttpStatus.OK)
  public Mono<Void> saveOneIdProfile(@RequestBody OneIdProfileSaveRequest request) {
    return oneIdProfileService.save(request.userId(), request.data()).then();
  }

  // OneID passport ma'lumotlarini olish (UserController.getPassportInfo uchun).
  @GetMapping("/oneid/passport/{userId}")
  public Mono<OneIdPassportResponse> getOneIdPassport(@PathVariable UUID userId) {
    return oneIdProfileService.getPassport(userId);
  }

  // SSO login URL — frontend foydalanuvchini shu yerga yo'naltiradi.
  // Optional override'lar (scope/state/authMethods) — CompanyService.getUrl uchun:
  //   - PERSONAL kompaniya: scope=legal_info, state=customName
  //   - LEGAL kompaniya:    scope=user_info, state=customName, auth_methods=LEPKCSMETHOD
  @GetMapping("/oneid/url")
  public Mono<UrlResponse> getOneIdUrl(
      @RequestParam String redirectUrl,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String authMethods) {
    return oneIdHttpService
        .buildLoginUrl(redirectUrl, scope, state, authMethods)
        .map(UrlResponse::new);
  }

  // OneID code → user data atomic ayirboshlash (sso.egov.uz s2s).
  @PostMapping("/oneid/exchange")
  public Mono<OneIdUserResponse> exchangeOneIdCode(@RequestBody OneIdExchangeRequest request) {
    return oneIdHttpService.exchange(request.code(), request.redirectUri());
  }

  // E-IMZO: /backend/auth — main-service sertifikat orqali auth uchun chaqiradi.
  @PostMapping("/eimzo/auth")
  public Mono<EImzoAuthResponse> eImzoAuth(@RequestBody EImzoAuthRequest request) {
    return eImzoHttpService.auth(request.pkcs7(), request.ipAddress());
  }

  // E-IMZO: /backend/pkcs7/verify/attached — document-service hujjat imzosini tekshirish uchun.
  @PostMapping("/eimzo/verify-attached")
  public Mono<EImzoVerifyResponse> eImzoVerifyAttached(@RequestBody EImzoVerifyRequest request) {
    return eImzoHttpService.verifyAttached(request.pkcs7(), request.ipAddress(), request.host());
  }

  // E-IMZO: /frontend/timestamp/pkcs7 — TSA timestamp olish.
  @PostMapping("/eimzo/timestamp")
  public Mono<String> eImzoTimestamp(@RequestBody EImzoVerifyRequest request) {
    return eImzoHttpService.getTimestamp(request.pkcs7(), request.ipAddress(), request.host());
  }

  // MyID code → pinfl. Ikki xil oqim platformaga qarab:
  //  - MOBILE: mobil SDK session code'i /api/v1/sdk/data orqali user data'ga almashtiriladi
  //    (docs: SDK session flow). OAuth /access-token mobil SDK code'ini qabul qilmaydi.
  //  - WEB: web session auth_code'i /api/v1/oauth2/access-token → /users/me (WebSDK Method A).
  @PostMapping("/myid/verify")
  public Mono<MyIdVerifyResponse> myIdVerify(@RequestBody MyIdVerifyRequest request) {
    if (request.platform() == MyIdPlatform.MOBILE) {
      return myIdService
          .getUserData(request.code(), null, request.userId(), null, request.platform())
          .map(
              data -> {
                var profile = data.data() != null ? data.data().profile() : null;
                MyIdCommonData common = profile != null ? profile.commonData() : null;
                var doc = profile != null ? profile.docData() : null;
                var addr = profile != null ? profile.address() : null;
                String pinfl = common != null ? common.pinfl() : null;
                Double comparison = data.data() != null ? data.data().comparisonValue() : null;
                return new MyIdVerifyResponse(
                    pinfl,
                    comparison,
                    common != null ? common.firstName() : null,
                    common != null ? common.lastName() : null,
                    common != null ? common.middleName() : null,
                    doc != null ? doc.passData() : null,
                    doc != null ? doc.issuedBy() : null,
                    doc != null ? doc.issuedDate() : null,
                    doc != null ? doc.expiryDate() : null,
                    common != null ? common.birthDate() : null,
                    common != null ? common.birthPlace() : null,
                    common != null ? common.nationality() : null,
                    common != null ? common.citizenship() : null,
                    addr != null ? addr.permanentAddress() : null);
              });
    }
    return myIdService
        .oauthGetPinfl(request.code(), request.userId(), request.platform())
        .map(pinfl -> new MyIdVerifyResponse(pinfl, null));
  }

  // AbleID attempt tekshiruvi (s2s) — document/main servislari imzo/profil
  // tasdiqlashda chaqiradi: sessiya kimga ochilgan (PINFL) va statusi.
  @PostMapping("/able-id/verify")
  public Mono<uz.hesap.service.integration.model.ableid.AbleIdVerifyResponse> ableIdVerify(
      @RequestBody AbleIdLocalVerifyRequest request) {
    return ableIdService.verify(request.attemptId());
  }

  public record AbleIdLocalVerifyRequest(String attemptId) {}

  public record UrlResponse(String url) {}

  // Xarid yozish (paket/tarif) — main chaqiradi. user.purchases jurnaliga qator qo'shadi.
  @PostMapping("/purchases")
  public Mono<Void> recordPurchase(@RequestBody PurchaseRecordRequest req) {
    uz.hesap.service.integration.domain.enums.PurchaseType type;
    try {
      type = uz.hesap.service.integration.domain.enums.PurchaseType.valueOf(req.unitType());
    } catch (Exception e) {
      type = uz.hesap.service.integration.domain.enums.PurchaseType.PACKAGE;
    }
    return purchaseService.record(
        req.userIn(), req.amount(), req.promo(), req.method(), type, req.unitId(), req.createdBy());
  }

  public record PurchaseRecordRequest(
      String userIn,
      Double amount,
      String promo,
      uz.hesap.service.common.util.enums.PaymentMethod method,
      String unitType,
      java.util.UUID unitId,
      java.util.UUID createdBy) {}
}
