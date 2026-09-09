package uz.hesap.service.main.api.v1;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.domain.UserScoringEntity;
import uz.hesap.service.main.service.SystemSettingService;
import uz.hesap.service.main.service.c2c.UserScoringService;

// Skoring narxlari va tarixi — mijoz (iOS) uchun.
@RestController
@RequestMapping("/main/v1/scoring")
@RequiredArgsConstructor
public class ScoringController {

  private final SystemSettingService systemSettingService;
  private final UserScoringService userScoringService;

  public record ScoringPricesDto(long hesap, long katm, long payment) {}

  // Bitta scoring jurnali qatori (partner tarixi uchun).
  public record ScoringHistoryDto(
      String id, String scoringId, String scoringType, String userIn, Instant createdAt) {}

  // Skoring turlari narxlari: Hesap, KATM, To'lov skoringi (so'm).
  @GetMapping("/prices")
  public Mono<ScoringPricesDto> getPrices() {
    return systemSettingService
        .getScoringPrices()
        .map(p -> new ScoringPricesDto(p.hesap(), p.katm(), p.payment()));
  }

  // Joriy foydalanuvchi (so'rovchi) shu odamni (userIn=PINFL/STIR) qilgan scoringlari.
  // Partner Info -> Scoring ekranida "Tarix" ro'yxati.
  @GetMapping("/history")
  public Flux<ScoringHistoryDto> history(
      @AuthenticationPrincipal UserPrincipal principal, @RequestParam String userIn) {
    return userScoringService
        .byRequesterAndUser(principal.user().identifier(), userIn)
        .map(this::toDto);
  }

  private ScoringHistoryDto toDto(UserScoringEntity e) {
    return new ScoringHistoryDto(
        e.getId() != null ? e.getId().toString() : null,
        e.getScoringId(),
        e.getScoringType(),
        e.getUserIn(),
        e.getCreatedAt());
  }
}
