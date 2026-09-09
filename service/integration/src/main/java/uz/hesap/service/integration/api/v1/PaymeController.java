package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.payme.CheckoutLinkModel;
import uz.hesap.service.integration.model.payme.Receive;
import uz.hesap.service.integration.service.payment.PaymeService;
import uz.hesap.service.integration.util.PaymeInterface;

@Log4j2
@RestController
@RequestMapping("/integration/v1/payme")
@RequiredArgsConstructor
public class PaymeController {
  private final PaymeService paymeService;

  @GetMapping("/link")
  public Mono<CheckoutLinkModel> getLink(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam Integer amount) {
    return paymeService.generateCheckoutLink(userPrincipal.user().company().id(), amount);
  }

  @GetMapping("/checkout")
  public Mono<CheckoutLinkModel> getCheckout(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam Integer amount) {
    return paymeService.generateCheckoutLink(userPrincipal.user().id(), amount);
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  // Jackson bo'sh marker interface (PaymeInterface) uchun encoder topolmaydi —
  // Object'ga cast qilinsa konkret javob klassi bo'yicha serialize bo'ladi.
  public Mono<Object> processPayment(
      @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
      @RequestBody Receive receive) {
    log.info("Payme webhook header [{}] request [{}] ", authorizationHeader, receive);
    return paymeService.processWithMerchantId(authorizationHeader, receive).cast(Object.class);
  }
}
