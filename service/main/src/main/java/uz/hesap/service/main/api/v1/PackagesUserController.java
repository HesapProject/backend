package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.model.UserPackageResponse;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.c2c.UserPackageService;

// Foydalanuvchi paketlari (packages-user). Eski /user-packages CRUD o'rniga faqat:
// GET — o'ziniki (admin/super_admin ?userId bilan boshqaniki), DELETE — faqat admin/super_admin.
@RestController
@RequestMapping("/main/v1/packages-user")
@RequiredArgsConstructor
public class PackagesUserController {

  private final UserPackageService userPackageService;
  private final UserRepository userRepository;

  // Mening paketlarim. Admin/super_admin ?pinfl (yoki ?userId) bilan bir kishini filtrlashi mumkin.
  @GetMapping
  public Mono<Page<UserPackageResponse>> list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String pinfl,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable = PageRequest.of(page, size);
    // Client/company — faqat o'ziniki (PINFL bo'yicha).
    if (!isAdmin(principal)) {
      return userPackageService.list(principal.user().in(), pageable);
    }
    // Admin — PINFL bilan to'g'ridan-to'g'ri filtr.
    if (pinfl != null && !pinfl.isBlank()) {
      return userPackageService.list(pinfl, pageable);
    }
    // Admin — userId (backward-compat): PINFL'ga aylantiramiz.
    if (userId != null) {
      return userRepository
          .findByIdAndDeletedIsFalse(userId)
          .flatMap(u -> userPackageService.list(u.getIn(), pageable))
          .defaultIfEmpty(Page.empty(pageable));
    }
    return userPackageService.list(null, pageable);
  }

  // O'chirish — faqat admin/super_admin. Id path'da: DELETE /packages-user/{id}.
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
    if (!isAdmin(principal)) {
      return Mono.error(new ForbiddenException("Only admin can delete user package"));
    }
    return userPackageService.delete(id);
  }

  private boolean isAdmin(UserPrincipal principal) {
    UserType type = principal.user().type();
    return type == UserType.ADMIN || type == UserType.SUPER_ADMIN;
  }
}
