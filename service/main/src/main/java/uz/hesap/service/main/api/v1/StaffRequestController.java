package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.StaffRequest;
import uz.hesap.service.main.model.response.StaffResponse;
import uz.hesap.service.main.model.tariff.IdRequest;
import uz.hesap.service.main.service.StaffService;

// Xodimlik taklifi oqimi — WhiteListRequestController'dek. Biznes fuqaroni xodimlikka
// tanlaganda unga PENDING so'rov boradi; qabul qilsa ACCEPTED (xodim bo'ladi).
@Log4j2
@RestController
@RequestMapping("/main/v1")
@RequiredArgsConstructor
public class StaffRequestController {

  private final StaffService staffService;

  // Taklif yuborish (type, fromCompanyId, toUserId, permissions body'da).
  @PostMapping("/staff-request")
  public Mono<StaffResponse> createRequest(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody StaffRequest request) {
    return staffService.createRequest(request, actorId(principal));
  }

  // So'rovlar ro'yxati. type=outgoing — o'zi yuborganlari; aks holda (incoming) — o'ziga
  // kelgan takliflar.
  @GetMapping("/staff-request")
  public Flux<StaffResponse> getRequests(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(required = false, defaultValue = "incoming") String type) {
    final UUID userId = principal.user().id();
    return "outgoing".equalsIgnoreCase(type)
        ? staffService.outgoingRequests(userId)
        : staffService.incomingRequests(userId);
  }

  // O'zi yuborgan taklifni bekor qilish (id body'da).
  @DeleteMapping("/staff-request")
  public Mono<Void> cancelRequest(@RequestBody IdRequest request) {
    return staffService.cancelRequest(request.id());
  }

  // Kelgan taklifni qabul qilish -> xodim bo'ladi (id body'da).
  @PostMapping("/staff-accept")
  public Mono<StaffResponse> acceptRequest(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody IdRequest request) {
    return staffService.acceptRequest(request.id(), principal.user().id());
  }

  // Kelgan taklifni rad etish (id body'da).
  @PostMapping("/staff-reject")
  public Mono<StaffResponse> rejectRequest(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody IdRequest request) {
    return staffService.rejectRequest(request.id(), principal.user().id());
  }

  private static UUID actorId(UserPrincipal principal) {
    return principal == null || principal.user() == null ? null : principal.user().id();
  }
}
