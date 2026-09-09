package uz.hesap.service.integration.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.ableid.AbleIdSessionResponse;
import uz.hesap.service.integration.model.ableid.AbleIdVerifyResponse;
import uz.hesap.service.integration.service.AbleIdService;

// AbleID identifikatsiya sessiyalari (mobil SDK uchun) + webhook.
@RestController
@RequestMapping("/integration/v1/able-id")
@RequiredArgsConstructor
public class AbleIdController {

  private final AbleIdService service;

  // Joriy foydalanuvchi (PINFL JWT'dan) uchun sessiya ochadi.
  @PostMapping("/session")
  public Mono<AbleIdSessionResponse> createSession(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(required = false) String lang) {
    return service.createSession(principal.user().identifier(), lang);
  }

  // SDK tugagach klient statusni tekshiradi (webhook kelganmi).
  @GetMapping("/status/{attemptId}")
  public Mono<AbleIdVerifyResponse> status(@PathVariable String attemptId) {
    return service.verify(attemptId);
  }

  // AbleID webhook (public, SecurityConfig'da permitAll) — hash ichkarida tekshiriladi.
  @PostMapping("/hook")
  public Mono<Void> hook(@RequestBody JsonNode payload) {
    return service.handleHook(payload);
  }
}
