package uz.hesap.service.main.service.c2c;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.main.domain.UserPackageEntity;
import uz.hesap.service.main.domain.enums.UserPackageStatus;
import uz.hesap.service.main.model.PackageResponse;
import uz.hesap.service.main.model.tariff.IdRequest;
import uz.hesap.service.main.model.UserPackageDetailResponse;
import uz.hesap.service.main.model.UserTemplateUsageResponse;
import uz.hesap.service.main.model.UserPackageResponse;
import uz.hesap.service.main.repository.UserPackageRepository;
import uz.hesap.service.main.service.PackageService;

// Foydalanuvchi paketi (user_package): xaridda yaratish, aktiv paketlar va CRUD.
@Service
@RequiredArgsConstructor
@Log4j2
public class UserPackageService {

  private final UserPackageRepository userPackageRepository;
  private final PackageService packageService;
  private final UserPackageUsageService usageService;
  private final ScoringUsageService scoringUsageService;
  private final uz.hesap.service.main.feign.IntegrationServiceClient integrationServiceClient;

  // --- Paket biriktirish ---
  // Balans olib tashlangani uchun xarid endi grant: user_package yozuvi yaratiladi va
  // xarid tarixi yoziladi. method faqat tarixda to'lov turini belgilash uchun (CONTROL/BALANCE).
  @Transactional
  public Mono<Void> purchasePackage(
      String userIn, UUID createdBy, IdRequest request, String promoCode, PaymentMethod method) {
    return packageService
        .getById(request.id())
        .switchIfEmpty(Mono.error(new NotFoundException("Package not found")))
        .flatMap(
            pkg ->
                resolveEffectivePrice(pkg, promoCode)
                    .flatMap(
                        price ->
                            // BALANCE bo'lsa avval integration balansidan yechamiz
                            // (yetmasa xato → xarid bo'lmaydi); CONTROL grant — yechmaydi.
                            // price==0 (to'liq promo chegirmasi yoki bepul paket) —
                            // yechish yo'q (0 summa withdraw'da 400 berardi), grant bo'ladi.
                            (method == PaymentMethod.BALANCE && price != null && price > 0
                                    ? integrationServiceClient.withdrawBalance(
                                        createdBy, price, "PACKAGE", "Paket xaridi")
                                    : Mono.<Void>empty())
                                .then(createForPurchase(userIn, pkg))
                                .then(
                                    integrationServiceClient.recordPurchase(
                                        userIn,
                                        price,
                                        promoCode,
                                        method,
                                        "PACKAGE",
                                        pkg.id(),
                                        createdBy))))
        .then();
  }

  // To'g'ridan-to'g'ri Payme/Click paket xaridi: narxni server tomonda hisoblab
  // (client buzolmaydi), integration'da PENDING order + checkout link oladi. To'lov
  // muvaffaqiyatli bo'lgach integration webhook'i grant'ni chaqiradi (balanssiz).
  public Mono<uz.hesap.service.main.feign.IntegrationServiceClient.UrlResponse> createCheckout(
      String userIn, UUID userId, UUID packageId, String promoCode, String provider) {
    return packageService
        .getById(packageId)
        .switchIfEmpty(Mono.error(new NotFoundException("Package not found")))
        .flatMap(
            pkg ->
                resolveEffectivePrice(pkg, promoCode)
                    .flatMap(
                        price ->
                            integrationServiceClient.createPaymentOrder(
                                userId,
                                userIn,
                                packageId,
                                (int) Math.round(price),
                                promoCode,
                                provider)));
  }

  // Promokod qo'llangach to'lanadigan narx: PERCENT → narxning foizi, FIXED → aniq summa.
  // promoCode bo'sh yoki yaroqsiz bo'lsa to'liq narx qaytadi.
  private Mono<Double> resolveEffectivePrice(PackageResponse pkg, String promoCode) {
    double price = pkg.price() != null ? pkg.price() : 0;
    if (promoCode == null || promoCode.isBlank()) {
      return Mono.just(price);
    }
    return integrationServiceClient
        .checkPromo(promoCode)
        .map(
            check -> {
              if (!check.valid()) {
                return price;
              }
              return Math.max(0, price - computeDiscount(price, check.discountType(), check.discountAmount()));
            })
        .defaultIfEmpty(price);
  }

  private double computeDiscount(double price, String discountType, Double discountAmount) {
    double amount = discountAmount != null ? discountAmount : 0;
    if ("PERCENT".equals(discountType)) {
      return price * amount / 100.0;
    }
    return amount; // FIXED
  }


  // Xaridda user_package yozuvini yaratish.
  public Mono<Void> createForPurchase(String userIn, PackageResponse pkg) {
    UserPackageEntity up = new UserPackageEntity();
    up.setPackageId(pkg.id());
    up.setUserIn(userIn);
    up.setExpDate(Instant.now().plus(pkg.duration(), ChronoUnit.DAYS));
    return userPackageRepository.save(up).then();
  }

  // --- Paketlar status bo'yicha (mobil: /packages-purchase?status=active|archive) ---
  public Flux<UserPackageDetailResponse> getPackages(String userIn, UserPackageStatus status) {
    Flux<UserPackageEntity> flux =
        status == UserPackageStatus.ARCHIVE
            ? userPackageRepository.findAllByUserInAndDeletedFalseAndExpDateBeforeOrderByCreatedAtDesc(
                userIn, Instant.now())
            : userPackageRepository.findAllByUserInAndDeletedFalseAndExpDateAfterOrderByCreatedAtDesc(
                userIn, Instant.now());
    return flux.flatMap(up -> toDetail(up).onErrorResume(e -> Mono.empty()));
  }

  private Mono<UserPackageDetailResponse> toDetail(UserPackageEntity up) {
    return packageService
        .getById(up.getPackageId())
        .flatMap(
            pkg -> {
              var config = pkg.templateConfig();
              var configTemplates =
                  config != null && config.templates() != null
                      ? config.templates()
                      : java.util.List.<uz.hesap.service.common.util.TariffTemplateResponse>of();
              // Har template uchun ishlatilgan son = user_package_usage'dagi ACTIVE qatorlar.
              return Flux.fromIterable(configTemplates)
                  .concatMap(
                      t -> {
                        UUID tid = t.template() != null ? t.template().id() : null;
                        Mono<Long> used =
                            tid != null
                                ? usageService.usedCount(up.getId(), tid, false)
                                : Mono.just(0L);
                        return used.map(
                            u ->
                                new UserTemplateUsageResponse(
                                    t.template(), t.count(), u.intValue()));
                      })
                  .collectList()
                  .flatMap(
                      usages ->
                          // DIQQAT: scoring usage packageId = user_package id (up.getId(),
                          // ya'ni balanceId) — recordScoringUsage shuni yozadi. pkg.id()
                          // (shablon paket id) bilan sanasak count doim 0 chiqadi.
                          scoringUsageService
                              .usedCount(up.getId(), "PAYMENT")
                              .map(
                                  scoringUsed ->
                                      new UserPackageDetailResponse(
                                          up.getId(),
                                          "PACKAGE",
                                          pkg.name(),
                                          pkg.description(),
                                          pkg.price(),
                                          pkg.stars(),
                                          pkg.duration(),
                                          up.getExpDate(),
                                          up.getCreatedAt(),
                                          config != null ? config.type() : null,
                                          config != null ? config.totalCount() : null,
                                          pkg.scoringHesap(),
                                          pkg.scoringKatm(),
                                          pkg.scoringPayment(),
                                          scoringUsed.intValue(),
                                          usages)));
            });
  }

  // --- CRUD ---
  public Mono<Page<UserPackageResponse>> list(String userIn, Pageable pageable) {
    boolean filter = userIn != null && !userIn.isBlank();
    Flux<UserPackageEntity> flux =
        filter
            ? userPackageRepository.findAllByUserInAndDeletedFalseOrderByCreatedAtDesc(userIn)
            : userPackageRepository.findAllByDeletedFalseOrderByCreatedAtDesc(pageable);
    Mono<Long> count =
        filter
            ? userPackageRepository.countByUserInAndDeletedFalse(userIn)
            : userPackageRepository.countByDeletedFalse();
    return flux.map(this::toResponse)
        .collectList()
        .zipWith(count)
        .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  public Mono<Void> delete(UUID id) {
    return userPackageRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("User package not found")))
        .flatMap(
            up -> {
              up.setDeleted(Boolean.TRUE);
              return userPackageRepository.save(up);
            })
        .then();
  }

  private UserPackageResponse toResponse(UserPackageEntity up) {
    return new UserPackageResponse(
        up.getId(),
        up.getPackageId(),
        up.getUserIn(),
        up.getExpDate(),
        up.getCreatedAt(),
        up.getUpdatedAt());
  }
}
