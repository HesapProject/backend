package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;
import uz.hesap.service.document.model.request.PaymentRequestActionRequest;
import uz.hesap.service.document.model.request.ProductRequestCreateRequest;
import uz.hesap.service.document.model.response.ProductRequestResponse;
import uz.hesap.service.document.service.c2c.ProductRequestsService;

// Mahsulot so'rovlari (product request): yaratish, tasdiq, rad, bekor, ro'yxat.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/product-requests")
public class ProductRequestsController {

  private final ProductRequestsService service;

  // Yaratish — body'dagi contractId + productId + ixtiyoriy reason.
  @PostMapping
  public Mono<Void> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody ProductRequestCreateRequest request) {
    return service.create(userPrincipal, request);
  }

  // Tasdiqlash (approve) — qarshi taraf. id body'da.
  @PostMapping("/approve")
  public Mono<Void> approve(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentRequestActionRequest request) {
    return service.approve(userPrincipal, request.id());
  }

  // Rad etish (reject) — qarshi taraf. id body'da.
  @PostMapping("/reject")
  public Mono<Void> reject(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentRequestActionRequest request) {
    return service.reject(userPrincipal, request.id());
  }

  // Bekor qilish (cancel) — so'rovchi o'z so'rovini bekor qiladi. id body'da.
  @PostMapping("/cancel")
  public Mono<Void> cancel(@RequestBody PaymentRequestActionRequest request) {
    return service.cancel(request.id());
  }

  // Filtrlangan ro'yxat: ?buyerIn, ?sellerIn, ?fromIn (men yuborgan=requester_in),
  // ?toIn (menga kelgan), ?contractId, ?statuses.
  @GetMapping
  public Flux<ProductRequestResponse> getProductRequests(
      @RequestParam(required = false) String buyerIn,
      @RequestParam(required = false) String sellerIn,
      @RequestParam(required = false) String fromIn,
      @RequestParam(required = false) String toIn,
      @RequestParam(required = false) UUID contractId,
      @RequestParam(required = false) List<ProductRequestStatus> statuses) {
    return service.getFiltered(buyerIn, sellerIn, fromIn, toIn, contractId, statuses);
  }
}
