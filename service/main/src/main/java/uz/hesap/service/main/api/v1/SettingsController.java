package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.service.SystemSettingService;

// Admin "Sozlamalar" bo'limi: tizim sozlamalari (hozircha telefon tekshiruvi switch'i).
@RestController
@RequestMapping("/main/v1/admin/settings")
@RequiredArgsConstructor
public class SettingsController {

  private final SystemSettingService systemSettingService;

  public record ToggleResponse(boolean enabled) {}

  public record ToggleRequest(boolean enabled) {}

  public record ScoringPricesDto(long hesap, long katm, long payment) {}

  // Skoring narxlari (so'm) — admin o'qiydi.
  @GetMapping("/scoring")
  public Mono<ScoringPricesDto> getScoringPrices() {
    return systemSettingService
        .getScoringPrices()
        .map(p -> new ScoringPricesDto(p.hesap(), p.katm(), p.payment()));
  }

  // Skoring narxlarini saqlash (so'm).
  @PutMapping("/scoring")
  public Mono<ScoringPricesDto> setScoringPrices(@RequestBody ScoringPricesDto request) {
    return systemSettingService
        .setScoringPrices(request.hesap(), request.katm(), request.payment())
        .thenReturn(request);
  }

  public record IdentityProviderDto(String provider) {}

  // Shaxs tasdiqlash provayderi (MY_ID | ABLE_ID).
  @GetMapping("/identity-provider")
  public Mono<IdentityProviderDto> getIdentityProvider() {
    return systemSettingService.getIdentityProvider().map(IdentityProviderDto::new);
  }

  // Shaxs tasdiqlash provayderini o'rnatish.
  @PutMapping("/identity-provider")
  public Mono<IdentityProviderDto> setIdentityProvider(@RequestBody IdentityProviderDto request) {
    return systemSettingService
        .setIdentityProvider(request.provider())
        .then(systemSettingService.getIdentityProvider())
        .map(IdentityProviderDto::new);
  }

  // Telefon raqam tekshiruvi (Turanix) yoniqmi.
  @GetMapping("/phone-check")
  public Mono<ToggleResponse> getPhoneCheck() {
    return systemSettingService.isTuranixPhoneCheckEnabled().map(ToggleResponse::new);
  }

  // Telefon raqam tekshiruvini yoqish/o'chirish.
  @PutMapping("/phone-check")
  public Mono<ToggleResponse> setPhoneCheck(@RequestBody ToggleRequest request) {
    return systemSettingService
        .setTuranixPhoneCheckEnabled(request.enabled())
        .thenReturn(new ToggleResponse(request.enabled()));
  }
}
