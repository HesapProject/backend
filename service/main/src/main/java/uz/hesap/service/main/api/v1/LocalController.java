package uz.hesap.service.main.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.*;
import uz.hesap.service.main.model.PackageUsageRequest;
import uz.hesap.service.main.model.ScoringUsageRequest;
import uz.hesap.service.main.model.UserScoringRequest;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.CompanyService;
import uz.hesap.service.main.service.SessionService;
import uz.hesap.service.main.service.UserService;
import uz.hesap.service.main.service.c2c.ScoringUsageService;
import uz.hesap.service.main.service.c2c.UserScoringService;
import uz.hesap.service.main.service.c2c.UserPackageUsageService;

@Log4j2
@RestController
@RequestMapping("/main/v1/local")
@RequiredArgsConstructor
public class LocalController {

  private final UserService userService;
  private final CompanyService companyService;
  private final SessionService sessionService;
  private final UserRepository userRepository;
  private final UserPackageUsageService userPackageUsageService;
  private final ScoringUsageService scoringUsageService;
  private final UserScoringService userScoringService;
  private final uz.hesap.service.main.service.c2c.UserPackageService userPackageService;
  private final uz.hesap.service.main.service.ApiKeyService apiKeyService;

  // Id'lar bo'yicha to'liq user ma'lumoti (in + passport fieldlari bilan) — s2s universal lookup.
  @PostMapping("/users")
  public Flux<UserResponse> getByIds(@RequestBody UserIdsRequest request) {
    return userService.getUsersIdIn(request.userIds());
  }

  // PINFL/STIR bo'yicha to'liq user (passport fieldlari ham — alohida passport endpoint kerakmas).
  @GetMapping("/users/{in}")
  public Mono<UserResponse> getUserByIn(@PathVariable String in) {
    return userService.getUserByIn(in);
  }

  // CLIENT/COMPANY turini aniqlash (billing oqimi).
  @GetMapping("/{uniqueId}/type")
  public Mono<String> getCompanyUserType(@PathVariable UUID uniqueId) {
    log.debug("Getting user or company by unique id: {}", uniqueId);
    return companyService.getUserTypeByCompanyId(uniqueId);
  }

  // Foydalanuvchining FCM tokenlari — PINFL bo'yicha (push uchun).
  @GetMapping("/sessions")
  public Mono<List<String>> getSessionFcmTokens(@RequestParam String in) {
    return userRepository
        .findFirstByInAndDeletedFalseOrderByCreatedDateAsc(in)
        .flatMapMany(u -> sessionService.findUserFirebaseTokens(u.getId()))
        .collectList();
  }

  // Shartnoma tuzilganda paket foydalanishini qayd etish (document servisi chaqiradi).
  // Mos aktiv paket topilsa user_package_usage'ga bitta ACTIVE qator qo'shiladi.
  @PostMapping("/package-usage")
  public Mono<Void> recordPackageUsage(@RequestBody PackageUsageRequest request) {
    return userPackageUsageService.recordUsage(
        request.userId(), request.userPackageId(), request.templateId(), request.contractId());
  }

  // Scoring ishlatilishini qayd etish (integration servisi scoring create'dan keyin chaqiradi).
  @PostMapping("/scoring-usage")
  public Mono<Void> recordScoringUsage(@RequestBody ScoringUsageRequest request) {
    return scoringUsageService.record(request);
  }

  // Umumlashgan scoring jurnaliga yozish (usage EMAS — jamlash). Integration chaqiradi.
  @PostMapping("/user-scoring")
  public Mono<Void> recordUserScoring(@RequestBody UserScoringRequest request) {
    return userScoringService.record(request);
  }

  // To'g'ridan-to'g'ri Payme/Click to'lovi tasdiqlangach paketni biriktirish (integration
  // webhook s2s chaqiradi). method=BALANCE bo'lsa endigina to'ldirilgan balansdan yechadi
  // (net nol); boshqa method'da yechmaydi. userIn — kimga, userId — createdBy.
  @PostMapping("/packages/grant")
  public Mono<Void> grantPackage(@RequestBody PackageGrantRequest req) {
    uz.hesap.service.common.util.enums.PaymentMethod method;
    try {
      method = uz.hesap.service.common.util.enums.PaymentMethod.valueOf(req.method());
    } catch (Exception e) {
      method = uz.hesap.service.common.util.enums.PaymentMethod.BALANCE;
    }
    return userPackageService.purchasePackage(
        req.userIn(),
        req.userId(),
        new uz.hesap.service.main.model.tariff.IdRequest(req.packageId()),
        req.promoCode(),
        method);
  }

  public record PackageGrantRequest(
      String userIn, UUID userId, UUID packageId, String promoCode, String method) {}

  // X-API-Key -> kalit egasi + huquqlari (document servis auth converteri chaqiradi).
  // Kalit body'da yuboriladi — URL/query'da qolib log'ga tushmasligi uchun.
  @PostMapping("/api-keys/resolve")
  public Mono<ApiKeyResolveResponse> resolveApiKey(@RequestBody ApiKeyResolveRequest request) {
    return apiKeyService.resolve(request.key());
  }

  // Berilgan taraflar (PINFL/STIR) bo'yicha webhook manzillari — integration servis
  // WebhookEvent'ni kimga yuborishni shu ro'yxatdan aniqlaydi.
  @PostMapping("/api-keys/webhooks")
  public Flux<WebhookTargetResponse> webhookTargets(@RequestBody WebhookTargetsRequest request) {
    return apiKeyService.webhookTargets(request.ins());
  }

  public record ApiKeyResolveRequest(String key) {}

  public record WebhookTargetsRequest(List<String> ins) {}

  // Kartalar endi faqat integration servisda (billing.plum_cards) — main saqlamaydi.
}
