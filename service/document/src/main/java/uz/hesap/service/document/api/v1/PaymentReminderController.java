package uz.hesap.service.document.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.document.schedule.PaymentCronService;

// Admin: to'lov eslatmasi cron'ni qo'lda ishga tushirish (Monitoring UI'dan).
// Ushbu endpoint cron rejasidan mustaqil — kunlik 09:00 cron ham davom etadi.
@Log4j2
@RestController
@RequestMapping("/document/v1/admin/payment-reminder")
@RequiredArgsConstructor
public class PaymentReminderController {

  private final PaymentCronService paymentCronService;

  // Trigger — cron'ni hoziroq subscribe qilib qaytadi (fire-and-forget), audit yozuv
  // log servisda paydo bo'ladi. Response: 202 ACCEPTED.
  @PostMapping("/trigger")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Mono<Void> trigger(@AuthenticationPrincipal UserPrincipal principal) {
    return requireAdmin(principal)
        .doOnSuccess(
            ignored -> {
              log.info("Manual payment reminder trigger by admin");
              paymentCronService.sendPaymentReminders();
            });
  }

  private Mono<Void> requireAdmin(UserPrincipal principal) {
    UserType type = principal != null ? principal.user().type() : null;
    if (type != UserType.ADMIN && type != UserType.SUPER_ADMIN) {
      return Mono.error(new ForbiddenException("Admin huquqi kerak"));
    }
    return Mono.empty();
  }
}
