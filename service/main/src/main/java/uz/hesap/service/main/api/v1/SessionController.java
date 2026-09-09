package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.SessionResponse;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.model.response.SessionAdminResponse;
import uz.hesap.service.main.model.tariff.IdRequest;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.SessionService;

/** Foydalanuvchi sessiyalari — qurilmalar, logout, sessiyani tugatish. */
@RestController
@RequestMapping("/main/v1/session")
@RequiredArgsConstructor
public class SessionController {

  private final SessionService sessionService;
  private final UserRepository userRepository;

  /** Admin (Control): foydalanuvchining BARCHA sessiyalari PINFL bo'yicha (aktiv + arxiv). */
  @GetMapping("/admin")
  public Flux<SessionAdminResponse> adminSessions(@RequestParam final String pinfl) {
    return userRepository
        .findFirstByInAndDeletedFalseOrderByCreatedDateAsc(pinfl)
        .flatMapMany(u -> sessionService.findAllForAdmin(u.getId()));
  }

  /** Joriy sessiyani tugatish (logout). */
  @PostMapping("/logout")
  public Mono<Void> logOut(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return sessionService.logout(userPrincipal);
  }

  /** Foydalanuvchining barcha faol sessiyalari. */
  @GetMapping
  public Flux<SessionResponse> findAllSessions(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return sessionService.findSessions(userPrincipal.user().id());
  }

  /**
   * Sessiyani tugatish — id body'da. Oddiy foydalanuvchi faqat o'zinikini, admin/super_admin
   * istalganini tugatadi.
   */
  @DeleteMapping
  public Mono<Void> killSession(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody final IdRequest request) {
    final UserType type = userPrincipal.user().type();
    final boolean isAdmin = type == UserType.ADMIN || type == UserType.SUPER_ADMIN;
    return isAdmin
        ? sessionService.killSessionByAdmin(request.id())
        : sessionService.killSession(userPrincipal, request.id());
  }
}
