package uz.hesap.service.main.api.v1;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.request.AdminAuthRequest;
import uz.hesap.service.main.model.request.ClientOneIdVerifyRequest;
import uz.hesap.service.main.model.request.EImzoLoginRequest;
import uz.hesap.service.main.model.response.AdminAuthResponse;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.service.AuthService;
import uz.hesap.service.main.service.EImzoService;
import uz.hesap.service.main.service.OneIdService;

// Barcha login oqimlari bitta joyda (user/controllerdan alohida):
// admin login, OneID (jismoniy shaxs), E-IMZO.
@Log4j2
@RestController
@RequestMapping("/main/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OneIdService oneIdService;
  private final EImzoService eImzoService;

  // Admin / super_admin login (login + parol).
  @PostMapping("/login")
  public Mono<AdminAuthResponse> login(@RequestBody AdminAuthRequest request) {
    return authService.login(request);
  }

  // OneID login URL.
  @GetMapping("/one-id/url")
  public Mono<Map<String, String>> oneIdUrl(@RequestParam String redirectUrl) {
    return oneIdService.getUrl(redirectUrl).map(url -> Map.of("url", url));
  }

  // OneID callback verify -> JWT.
  @PostMapping("/one-id/verify")
  public Mono<JwtTokenResponse> oneIdVerify(@RequestBody ClientOneIdVerifyRequest request) {
    return oneIdService.verifyClient(request);
  }

  // E-IMZO login -> JWT.
  @PostMapping("/e-imzo")
  public Mono<JwtTokenResponse> eImzo(
      @RequestBody EImzoLoginRequest request, ServerHttpRequest httpRequest) {
    return eImzoService.auth(request, httpRequest);
  }
}
