package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.click.ClickCompleteResponse;
import uz.hesap.service.integration.model.click.ClickPrepareResponse;
import uz.hesap.service.integration.model.click.ClickResponse;
import uz.hesap.service.integration.model.mapper.ClickMapper;
import uz.hesap.service.integration.model.payme.CheckoutLinkModel;
import uz.hesap.service.integration.service.payment.ClickService;

@Log4j2
@RestController
@RequestMapping("/integration/v1/click")
@RequiredArgsConstructor
public class ClickController {
  private final ClickService clickService;

  @GetMapping("/link")
  public Mono<CheckoutLinkModel> getLink(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam Integer amount) {
    return clickService.generateCheckoutLink(userPrincipal.user().company().id(), amount);
  }

  @GetMapping("/checkout")
  public Mono<CheckoutLinkModel> getCheckout(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestParam Integer amount) {
    return clickService.generateCheckoutLink(userPrincipal.user().id(), amount);
  }

  @PostMapping(
      path = "/prepare",
      consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public Mono<ClickPrepareResponse> prepare(ClickResponse clickRequest) {
    log.debug("Click prepare request {} ", clickRequest);
    return clickService.prepare(clickRequest).map(ClickMapper.INSTANCE::toClickPrepare);
  }

  @PostMapping(
      path = "/complete",
      consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public Mono<ClickCompleteResponse> complete(ClickResponse clickRequest) {
    return clickService.complete(clickRequest).map(ClickMapper.INSTANCE::toClickComplete);
  }
}
