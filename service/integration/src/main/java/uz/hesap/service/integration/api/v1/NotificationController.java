package uz.hesap.service.integration.api;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.model.NotificationResponse;
import uz.hesap.service.integration.service.NotificationService;

@Log4j2
@RestController
@RequestMapping("/integration/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public Mono<Page<NotificationResponse>> getAllForUser(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
      @RequestParam(required = false) Sort sort) {
    Sort defaultSort = (sort != null) ? sort : Sort.by(Sort.Direction.DESC, "created_at");
    Pageable pageable = PageRequest.of(page, size, defaultSort);
    return notificationService.getAllForUser(userPrincipal, pageable);
  }

  // Barcha bildirishnomalarni o'qilgan qilib belgilash.
  @PutMapping("/read-all")
  public Mono<Void> markAllViewed(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return notificationService.markAllViewed(userPrincipal);
  }

  @GetMapping("/{id}")
  public Mono<NotificationResponse> getById(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable("id") UUID id) {
    return notificationService.getById(userPrincipal, id);
  }
}
