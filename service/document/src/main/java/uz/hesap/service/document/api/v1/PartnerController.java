package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.model.response.PartnerResponse;
import uz.hesap.service.document.service.c2c.PartnerService;

@RestController
@RequestMapping("/document/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

  private final PartnerService partnerService;

  // Hamkorlar ro'yxati:
  //   (param yo'q)      — JORIY foydalanuvchining hamkorlari
  //   ?in=<PINFL/STIR>  — o'sha tarafning hamkorlari (id emas, in ishonchliroq)
  @GetMapping
  public Flux<PartnerResponse> getPartners(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false) String in) {
    if (in != null && !in.isBlank()) {
      return partnerService.getPartnersByIn(in);
    }
    return partnerService.getPartners(userPrincipal.user().id());
  }

  // Aniq foydalanuvchining (mijozning) hamkorlari — profil "Hamkorlar" tabi uchun.
  @GetMapping("/{userId}")
  public Flux<PartnerResponse> getPartnersByUser(@PathVariable UUID userId) {
    return partnerService.getPartners(userId);
  }

  // O'zaro shartnomalar — userId va partnerId orasidagi (PartnerInfo "O'zaro shartnomalar" card).
  @GetMapping("/{userId}/with/{partnerId}")
  public Mono<Page<DocumentEnrichedResponse>> getContractsBetween(
      @PathVariable UUID userId, @PathVariable UUID partnerId, Pageable pageable) {
    return partnerService.getContractsBetween(userId, partnerId, pageable);
  }
}
