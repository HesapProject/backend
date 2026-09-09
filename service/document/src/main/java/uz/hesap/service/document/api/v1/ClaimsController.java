package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.request.ClaimCreateRequest;
import uz.hesap.service.document.model.response.ClaimResponse;
import uz.hesap.service.document.service.c2c.ClaimsService;

// Da'vo arizalari (claim) — hujjat bo'yicha da'vo yaratish va ro'yxat.
// MOSLIK: birlikdagi "/document/v1/claim" — App Store'da tarqalgan eski iOS
// build'lar shu manzilni chaqiradi (ilovani yangilamasdan ishlashi uchun).
@RestController
@RequiredArgsConstructor
@RequestMapping({"/document/v1/claims", "/document/v1/claim"})
public class ClaimsController {

  private final ClaimsService claimsService;

  // Menga kelgan da'vo arizalari (boshqa tomon yuborgan)
  // Da'vo arizalari filtri — ?fromIn (yuboruvchi PINFL), ?toIn (qabul qiluvchi PINFL).
  @GetMapping
  public Mono<Page<ClaimResponse>> getFiltered(
      @RequestParam(value = "fromIn", required = false) String fromIn,
      @RequestParam(value = "toIn", required = false) String toIn,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
    return claimsService.getFiltered(fromIn, toIn, PageRequest.of(page, size));
  }

  // document bo'yicha barcha da'vo arizalarini olish
  @GetMapping("/{documentId}")
  public Mono<Page<ClaimResponse>> getAll(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID documentId,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
    return claimsService.getAllByDocument(
        documentId, userPrincipal.user().id(), PageRequest.of(page, size));
  }

  @PostMapping
  public Mono<ClaimResponse> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody ClaimCreateRequest request) {
    return claimsService.create(request, userPrincipal.user().id());
  }
}
