package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.request.DelayRequestCreateRequest;
import uz.hesap.service.document.model.request.PaymentRequestActionRequest;
import uz.hesap.service.document.model.response.DelayRequestListResponse;
import uz.hesap.service.document.service.payment.DelayRequestService;

// Kechiktirish so'rovlari (delay request). To'lov so'rovlaridan alohida controller.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/delay-requests")
public class DelayRequestController {

  private final DelayRequestService service;

  // Yaratish — body'dagi paymentScheduleId + yangi sana.
  @PostMapping
  public Mono<Void> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody DelayRequestCreateRequest request) {
    return service.create(userPrincipal, request);
  }

  // Qabul qilish (approve) — id body'da.
  @PostMapping("/approve")
  public Mono<Void> approve(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentRequestActionRequest request) {
    return service.approve(userPrincipal, request.id());
  }

  // Rad etish (reject) — id body'da.
  @PostMapping("/reject")
  public Mono<Void> reject(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentRequestActionRequest request) {
    return service.reject(userPrincipal, request.id());
  }

  // Bekor qilish (cancel) — id body'da.
  @PostMapping("/cancel")
  public Mono<Void> cancel(@RequestBody PaymentRequestActionRequest request) {
    return service.cancel(request.id());
  }

  // Filtrlangan ro'yxat: ?buyerIn, ?sellerIn, ?fromIn (men yuborgan=buyer_in),
  // ?toIn (menga kelgan=seller_in), ?contractId, ?paymentId, ?statuses.
  @GetMapping
  public Flux<DelayRequestListResponse> getDelayRequests(
      @RequestParam(required = false) String buyerIn,
      @RequestParam(required = false) String sellerIn,
      @RequestParam(required = false) String fromIn,
      @RequestParam(required = false) String toIn,
      @RequestParam(required = false) UUID contractId,
      @RequestParam(required = false) UUID paymentId,
      @RequestParam(required = false) List<PaymentScheduleStatus> statuses) {
    return service.getDelayRequests(buyerIn, sellerIn, fromIn, toIn, contractId, paymentId, statuses);
  }
}
