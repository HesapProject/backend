package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import java.util.List;
import uz.hesap.service.document.model.request.WitnessAddRequest;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.model.response.WitnessResponse;
import uz.hesap.service.document.service.c2c.WitnessRequestsService;

// Guvohlik so'rovlari (witness_requests): guvohlikka chaqirilganda shu yerga tushadi.
// Qabul/rad qilish — WitnessController (/document/v1/witness) orqali.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/witness-requests")
public class WitnessRequestsController {

  private final WitnessRequestsService service;

  // Shartnoma bo'yicha guvohlik so'rovlari ro'yxati (status + guvoh ma'lumoti bilan).
  @GetMapping
  public Flux<WitnessResponse> getByContract(@RequestParam UUID contractId) {
    return service.getByContract(contractId);
  }

  // Menga kelgan guvohlik takliflari (men guvohman, PENDING) — Home incoming.
  @GetMapping("/incoming")
  public Mono<List<DocumentEnrichedResponse>> getIncoming(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return service.getIncoming(userPrincipal.user().id(), userPrincipal.user().identifier());
  }

  // Men guvohlik bergan (ACCEPTED) shartnomalar — More > Guvohliklar.
  @GetMapping("/mine")
  public Mono<List<DocumentEnrichedResponse>> getMine(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return service.getMine(userPrincipal.user().id(), userPrincipal.user().identifier());
  }

  // Men yuborgan guvohlik so'rovlari (men taraf, guvoh imzosi kutilyapti) — Home outgoing.
  @GetMapping("/outgoing")
  public Mono<List<DocumentEnrichedResponse>> getOutgoing(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return service.getOutgoing(userPrincipal.user().identifier());
  }

  // Imzolanmagan shartnomaga guvoh qo'shish (taraf tomonidan).
  @PostMapping
  public Mono<Void> addWitness(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody WitnessAddRequest request) {
    return service.addWitness(
        request.contractId(), request.witnessId(), userPrincipal.user().identifier());
  }

  // Hali imzolamagan guvohni olib tashlash (taraf tomonidan).
  @DeleteMapping
  public Mono<Void> removeWitness(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam UUID contractId,
      @RequestParam UUID witnessId) {
    return service.removeWitness(contractId, witnessId, userPrincipal.user().identifier());
  }
}
