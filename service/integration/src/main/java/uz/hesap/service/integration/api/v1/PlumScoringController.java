package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.plum.*;
import uz.hesap.service.integration.service.scoring.PlumService;

@RestController
@RequestMapping("/integration/v1/plum/scoring")
@RequiredArgsConstructor
public class PlumScoringController {

  private final PlumService plumService;

  //  @PostMapping("/uzcard/create")
  //  public Mono<CreateScoringResponse> createScoring(@RequestBody CreateScoringRequest request) {
  //    return plumService.createScoring(request);
  //  }
  //
  //  @PostMapping("/humo/create")
  //  public Mono<CreateScoringResponse> createHumoScoring(
  //      @RequestBody CreateHumoScoringRequest request) {
  //    return plumService.createHumoScoring(request);
  //  }
  //
  //  @PostMapping("/confirm")
  //  public Mono<ConfirmScoringResponse> confirmScoring(@RequestBody ConfirmScoringRequest request)
  // {
  //    return plumService.confirmScoring(request);
  //  }
  //
  //  @GetMapping("/uzcard/{scoringId}")
  //  public Mono<GetScoringResponse> getScoring(
  //      @PathVariable Integer scoringId, @AuthenticationPrincipal UserPrincipal user) {
  //    return plumService.getScoring(scoringId, user);
  //  }
  //
  //  @GetMapping("/humo/{scoringId}")
  //  public Mono<GetHumoScoringResponse> getHumoScoring(
  //      @PathVariable Integer scoringId, @AuthenticationPrincipal UserPrincipal user) {
  //    return plumService.getHumoScoring(scoringId, user);
  //  }

  @PostMapping("/create")
  public Mono<ScoringStatusResponse> createScoringCard(
      @RequestBody CreateScoringCardRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return plumService.createScoringCard(request, principal.user().identifier());
  }

  // Faqat MENING (so'rovchi) skoringlarim — boshqalar qilgan skoring ko'rinmaydi.
  @GetMapping("/history/my")
  public Flux<ScoringResponse> getMyHistory(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(required = false) UUID cardId,
      Pageable pageable) {
    return plumService.getMyScoringHistory(principal.user().identifier(), cardId, pageable);
  }

  @GetMapping("/{id}")
  public Mono<ScoringStatusResponse> getScoringStatus(@PathVariable UUID id) {
    return plumService.getScoringStatus(id);
  }

  // Tarix PINFL/STIR (user_in) bo'yicha.
  @GetMapping("/history/{userIn}")
  public Flux<ScoringResponse> getHistory(
      @PathVariable String userIn, @RequestParam(required = false) UUID cardId, Pageable pageable) {
    return plumService.getUserScoringHistory(userIn, cardId, pageable);
  }

  // Admin tab — scoring tarixi PINFL bilan (endi user_in to'g'ridan-to'g'ri, resolve'siz).
  @GetMapping("/history")
  public Flux<ScoringResponse> getHistoryByPinfl(
      @RequestParam String pinfl,
      @RequestParam(required = false) UUID cardId,
      Pageable pageable) {
    return plumService.getUserScoringHistory(pinfl, cardId, pageable);
  }
}
