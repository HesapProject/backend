package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.enums.UserPackageStatus;
import uz.hesap.service.main.model.PackagesPurchaseRequest;
import uz.hesap.service.main.model.UserPackageDetailResponse;
import uz.hesap.service.main.model.tariff.IdRequest;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.c2c.UserPackageService;

// Paket biriktirish (packages-purchase). Client/company — o'ziga; admin/super_admin — body'dagi
// userId uchun. Balans olib tashlangach xarid = grant (user_package + tarix).
@RestController
@RequestMapping("/main/v1/packages-purchase")
@RequiredArgsConstructor
public class PackagesPurchaseController {

  private final UserPackageService userPackageService;
  private final UserRepository userRepository;

  // Paket biriktirish. Admin/super_admin — body'dagi userId uchun (CONTROL);
  // boshqa foydalanuvchi — o'ziga (BALANCE turi tarixda qayd etiladi).
  // user_package endi PINFL (user_in) bilan bog'lanadi.
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Void> purchase(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody PackagesPurchaseRequest request) {
    var user = principal.user();
    if (isAdmin(user.type())) {
      if (request.userId() == null) {
        return Mono.error(new BadRequestException("userId is required for admin"));
      }
      // Admin grant — target user'ning PINFL'ini userId'dan resolish qilamiz; promo qo'llanmaydi.
      return userRepository
          .findByIdAndDeletedIsFalse(request.userId())
          .switchIfEmpty(Mono.error(new BadRequestException("User not found")))
          .flatMap(
              target ->
                  userPackageService.purchasePackage(
                      target.getIn(),
                      user.id(),
                      new IdRequest(request.packageId()),
                      null,
                      PaymentMethod.CONTROL));
    }
    return userPackageService.purchasePackage(
        user.in(),
        user.id(),
        new IdRequest(request.packageId()),
        request.promoCode(),
        PaymentMethod.BALANCE);
  }

  // To'g'ridan-to'g'ri Payme/Click paket xaridi uchun checkout link. Narx server tomonda
  // hisoblanadi (client buzolmaydi); to'lov muvaffaqiyatli bo'lgach paket avtomatik
  // biriktiriladi (balanssiz — integration webhook grant'ni chaqiradi).
  @PostMapping("/checkout")
  public Mono<uz.hesap.service.main.feign.IntegrationServiceClient.UrlResponse> checkout(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody PackageCheckoutRequest request) {
    var user = principal.user();
    String provider = request.provider() == null ? "PAYME" : request.provider();
    return userPackageService.createCheckout(
        user.in(), user.id(), request.packageId(), request.promoCode(), provider);
  }

  public record PackageCheckoutRequest(
      java.util.UUID packageId, String promoCode, String provider) {}

  // Userning paketlari status bo'yicha: active (default) yoki archive.
  @GetMapping
  public Flux<UserPackageDetailResponse> getPackages(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(defaultValue = "active") String status) {
    return userPackageService.getPackages(principal.user().in(), parseStatus(status));
  }

  private UserPackageStatus parseStatus(String status) {
    try {
      return UserPackageStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BadRequestException("Invalid status: " + status + " (active|archive)");
    }
  }

  private boolean isAdmin(UserType type) {
    return type == UserType.ADMIN || type == UserType.SUPER_ADMIN;
  }
}
