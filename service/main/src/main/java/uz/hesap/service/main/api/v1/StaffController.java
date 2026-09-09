package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.StaffRequest;
import uz.hesap.service.main.model.response.StaffResponse;
import uz.hesap.service.main.service.StaffService;

@RestController
@RequestMapping("/main/v1/staff")
@RequiredArgsConstructor
public class StaffController {

  private final StaffService staffService;

  @GetMapping
  public Flux<StaffResponse> list(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return staffService.list(userPrincipal.user().id());
  }

  @GetMapping("/{id}")
  public Mono<StaffResponse> getById(@PathVariable UUID id) {
    return staffService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<StaffResponse> create(
      @RequestBody StaffRequest request, @AuthenticationPrincipal UserPrincipal principal) {
    return staffService.create(request, actorId(principal));
  }

  @PutMapping("/{id}")
  public Mono<StaffResponse> update(
      @PathVariable UUID id,
      @RequestBody StaffRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return staffService.update(id, request, actorId(principal));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable UUID id) {
    return staffService.delete(id);
  }

  private static UUID actorId(UserPrincipal principal) {
    return principal == null || principal.user() == null ? null : principal.user().id();
  }
}
