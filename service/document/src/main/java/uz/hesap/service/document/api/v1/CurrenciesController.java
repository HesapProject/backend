package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.document.model.request.CurrencyRequest;
import uz.hesap.service.document.model.response.CurrencyResponse;
import uz.hesap.service.document.service.CurrenciesService;

// Valyutalar: public o'qish (active) + admin CRUD (JWT role bilan tekshiriladi).
@RestController
@RequestMapping("/document/v1/currencies")
@RequiredArgsConstructor
public class CurrenciesController {

  private final CurrenciesService currencyService;

  /** Public: faqat active valyutalar — FE dropdown uchun. */
  @GetMapping
  public Flux<CurrencyResponse> getAllActive() {
    return currencyService.getAllActive();
  }

  /** Admin: barcha valyutalar (active emaslar ham). */
  @GetMapping("/all")
  public Flux<CurrencyResponse> getAll(@AuthenticationPrincipal UserPrincipal principal) {
    return requireAdmin(principal).thenMany(currencyService.getAll());
  }

  @GetMapping("/{id}")
  public Mono<CurrencyResponse> getById(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
    return requireAdmin(principal).then(currencyService.getById(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<CurrencyResponse> create(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody CurrencyRequest request) {
    return requireAdmin(principal).then(currencyService.create(request));
  }

  @PutMapping("/{id}")
  public Mono<CurrencyResponse> update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable UUID id,
      @RequestBody CurrencyRequest request) {
    return requireAdmin(principal).then(currencyService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
    return requireAdmin(principal).then(currencyService.delete(id));
  }

  // JWT'dagi role ADMIN/SUPER_ADMIN emasligini tekshiradi.
  private Mono<Void> requireAdmin(UserPrincipal principal) {
    UserType type = principal != null ? principal.user().type() : null;
    if (type != UserType.ADMIN && type != UserType.SUPER_ADMIN) {
      return Mono.error(new ForbiddenException("Admin huquqi kerak"));
    }
    return Mono.empty();
  }
}
