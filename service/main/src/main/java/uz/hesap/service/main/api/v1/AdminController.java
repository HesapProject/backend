package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.model.request.AdminCreateRequest;
import uz.hesap.service.main.model.response.AdminResponse;
import uz.hesap.service.main.service.AdminService;

// Control admin akkauntlarini boshqarish. Faqat ADMIN/SUPER_ADMIN kira oladi —
// CLIENT token bilan admin yaratib bo'lmaydi (privilege escalation'dan himoya).
@RestController
@RequestMapping("/main/v1/admins")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  @GetMapping
  public Flux<AdminResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return requireAdmin(principal).thenMany(adminService.list());
  }

  @PostMapping
  public Mono<AdminResponse> create(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody AdminCreateRequest request) {
    return requireAdmin(principal).then(adminService.create(request));
  }

  @DeleteMapping("/{id}")
  public Mono<Void> delete(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
    return requireAdmin(principal).then(adminService.delete(id));
  }

  private Mono<Void> requireAdmin(UserPrincipal principal) {
    UserType type =
        principal != null && principal.user() != null ? principal.user().type() : null;
    if (type != UserType.ADMIN && type != UserType.SUPER_ADMIN) {
      return Mono.error(new ForbiddenException("Ruxsat yo'q"));
    }
    return Mono.empty();
  }
}
