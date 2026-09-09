package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.response.HesapScoreResponse;
import uz.hesap.service.main.service.c2c.HesapScoreService;

// Hesap Score — mijoz o'z skorini ko'radi (Faza 1, bepul). Boshqaning skorini
// ko'rish (Faza 2) — paket kvotasidan yechiladi (karta scoringidek billing).
@RestController
@RequestMapping("/main/v1")
@RequiredArgsConstructor
public class HesapScoreController {

  private final HesapScoreService hesapScoreService;

  // O'z Hesap Score'i (PINFL/STIR). Yo'q/stale bo'lsa sinxron hisoblaydi.
  @GetMapping("/hesap-score/me")
  public Mono<HesapScoreResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
    return hesapScoreService.getOrCompute(principal.user().identifier());
  }

  // Boshqa tomon skorini ko'rish — HESAP paket kvotasidan yechadi (karta scoringidek).
  // Kvota yo'q → 400 PACKAGE_REQUIRED / SCORING_QUOTA_EXHAUSTED.
  @PostMapping("/hesap-score/view")
  public Mono<HesapScoreResponse> view(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody ViewRequest req) {
    return hesapScoreService.viewForRequester(
        principal.user().identifier(), req.subjectIn(), req.userPackageId());
  }

  public record ViewRequest(String subjectIn, UUID userPackageId) {}

  // Tarixdan qayta ko'rish — pul yechmaydi (so'rovchi oldin so'ragan bo'lsa).
  @GetMapping("/hesap-score/view")
  public Mono<HesapScoreResponse> reviewStored(
      @AuthenticationPrincipal UserPrincipal principal, @RequestParam String userIn) {
    return hesapScoreService.reviewStored(principal.user().identifier(), userIn);
  }

  // Ichki (s2s / manual) — bitta PINFL/STIR uchun majburiy qayta hisoblash. /local = permitAll.
  @PostMapping("/local/hesap-score/recompute")
  public Mono<HesapScoreResponse> recompute(@RequestBody RecomputeRequest req) {
    return hesapScoreService.computeAndStore(req.userIn());
  }

  public record RecomputeRequest(String userIn) {}
}
