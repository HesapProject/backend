package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.PermissionActionRequest;
import uz.hesap.service.main.model.response.PermissionRequestResponse;
import uz.hesap.service.main.service.PermissionRequestService;

// White-list (ruxsat so'rovi) — eski /main/v1/friends o'rnini bosadi.
// Logika PermissionRequestService'da (o'zgarmagan), faqat URL'lar white-list-* ga ko'chirildi.
@Log4j2
@RestController
@RequestMapping("/main/v1")
@RequiredArgsConstructor
public class WhiteListRequestController {

  private final PermissionRequestService permissionRequestService;

  // Ruxsat so'rovi yuborish (targetUserId + ruxsat bayroqlari body'da).
  @PostMapping("/white-list-request")
  public Mono<PermissionRequestResponse> createRequest(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PermissionActionRequest request) {
    return permissionRequestService.createRequest(
        userPrincipal.user(), request.targetUserId(), request);
  }

  // So'rovlar ro'yxati. type=outgoing — o'zi yuborganlari; aks holda (incoming) — o'ziga
  // yuborilganlari.
  @GetMapping("/white-list-request")
  public Mono<Page<PermissionRequestResponse>> getRequests(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false, defaultValue = "incoming") String type,
      Pageable pageable) {
    final UUID userId = userPrincipal.user().id();
    return "outgoing".equalsIgnoreCase(type)
        ? permissionRequestService.getOutgoingRequests(userId, pageable)
        : permissionRequestService.getIncomingRequests(userId, pageable);
  }

  // O'zi yuborgan so'rovni bekor qilish (targetUserId body'da).
  @DeleteMapping("/white-list-request")
  public Mono<Void> cancelRequest(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PermissionActionRequest request) {
    return permissionRequestService.cancelRequest(
        userPrincipal.user().id(), request.targetUserId());
  }

  // Kelgan so'rovni qabul qilish (targetUserId — so'rov yuborgan user; ruxsatlar body'da).
  @PostMapping("/white-list-accept")
  public Mono<Void> acceptRequest(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PermissionActionRequest request) {
    return permissionRequestService.acceptRequest(
        userPrincipal.user(), request.targetUserId(), request);
  }

  // Kelgan so'rovni rad etish (targetUserId — so'rov yuborgan user).
  @PostMapping("/white-list-reject")
  public Mono<Void> rejectRequest(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PermissionActionRequest request) {
    return permissionRequestService.rejectRequest(userPrincipal.user(), request.targetUserId());
  }
}
