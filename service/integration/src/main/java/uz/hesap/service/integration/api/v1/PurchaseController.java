package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.PurchaseRequest;
import uz.hesap.service.integration.model.PurchaseResponse;
import uz.hesap.service.integration.service.PurchaseService;

// Xaridlar jurnali CRUD. Tarif/paket sotib olinganda avto-yoziladi; bu yer ko'rish/qo'lda boshqarish.
@RestController
@RequestMapping("/integration/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

  private final PurchaseService purchaseService;

  // Admin tab'lari foydalanuvchini PINFL bilan filtrlaydi.
  @GetMapping
  public Mono<Page<PurchaseResponse>> list(
      @RequestParam(required = false) String pinfl,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable = PageRequest.of(page, size);
    return purchaseService.list(pinfl, pageable);
  }

  // Admin Xaridlar sahifasi: server-side sahifalash + filtrlar (paket/promo/sana).
  @GetMapping("/paged")
  public Mono<Page<PurchaseResponse>> paged(
      @RequestParam(required = false) UUID unitId,
      @RequestParam(required = false) String promo,
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return purchaseService.listFiltered(unitId, promo, from, to, PageRequest.of(page, size));
  }

  // Statistika: sana oralig'idagi tushum (tashqi to'lov usullaridagi paket xaridlari
  // yig'indisi). from/to — ISO instant (masalan 2026-09-01T00:00:00Z); berilmasa cheksiz.
  @GetMapping("/revenue")
  public Mono<RevenueResponse> revenue(
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to) {
    return purchaseService.revenue(from, to).map(RevenueResponse::new);
  }

  public record RevenueResponse(Double total) {}

  @GetMapping("/{id}")
  public Mono<PurchaseResponse> getById(@PathVariable UUID id) {
    return purchaseService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<PurchaseResponse> create(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody PurchaseRequest request) {
    return purchaseService.create(principal.user().id(), request);
  }

  @PutMapping("/{id}")
  public Mono<PurchaseResponse> update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @RequestBody PurchaseRequest request) {
    return purchaseService.update(principal.user().id(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable UUID id) {
    return purchaseService.delete(id);
  }
}
