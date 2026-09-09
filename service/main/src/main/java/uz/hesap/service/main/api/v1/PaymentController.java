package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.PaymentRequest;
import uz.hesap.service.main.model.PaymentResponse;
import uz.hesap.service.main.service.PaymentService;

// To'lovlar jurnali CRUD. Payme/Click avto-yozadi; bu yer admin ko'rish/qo'lda boshqarish uchun.
@RestController
@RequestMapping("/main/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @GetMapping
  public Mono<Page<PaymentResponse>> list(
      @RequestParam(required = false) UUID userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return paymentService.list(userId, PageRequest.of(page, size));
  }

  @GetMapping("/{id}")
  public Mono<PaymentResponse> getById(@PathVariable UUID id) {
    return paymentService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<PaymentResponse> create(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody PaymentRequest request) {
    return paymentService.create(principal.user().id(), request);
  }

  @PutMapping("/{id}")
  public Mono<PaymentResponse> update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @RequestBody PaymentRequest request) {
    return paymentService.update(principal.user().id(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable UUID id) {
    return paymentService.delete(id);
  }
}
