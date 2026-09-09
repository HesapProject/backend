package uz.hesap.service.main.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.hesap.service.main.service.PermissionRequestService;

@Log4j2
@Component
@RequiredArgsConstructor
public class PermissionExpirationJob {

  private final PermissionRequestService permissionRequestService;

  // Har kuni tunda soat 03:00 da ishlaydi — O'zbekiston vaqti (Asia/Tashkent)
  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Tashkent")
  public void expireOldPermissions() {
    log.info("Starting cron job to expire old permissions at 03:00 AM...");
    permissionRequestService
        .expireAllPermissions()
        .subscribe(
            count ->
                log.info(
                    "Successfully expired old permissions. Row count (if retrieved): {}", count),
            error -> log.error("Error occurred while expiring old permissions", error));
  }
}
