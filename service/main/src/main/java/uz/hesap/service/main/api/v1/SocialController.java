package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.SocialRequest;
import uz.hesap.service.main.model.response.SocialResponse;
import uz.hesap.service.main.service.SocialService;

@Log4j2
@RestController
@RequestMapping("/main/v1/socials")
@RequiredArgsConstructor
public class SocialController {

  private final SocialService socialService;

  @PostMapping
  public Mono<SocialResponse> add(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody SocialRequest request) {
    return socialService.add(userPrincipal, request);
  }

  @GetMapping
  public Mono<SocialResponse> get(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return socialService.get(userPrincipal);
  }
}
