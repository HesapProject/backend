package uz.hesap.service.log.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.PaymentReminderLogEntity;
import uz.hesap.service.log.service.PaymentReminderLogService;

// S2S endpointlar — document servis kabi ichki servislar log jadvallariga yozish uchun.
// Public /log/v1/logs/* dan farqi: bu WebClient orqali chaqiriladi (auth yumshoq),
// audit yozuvlarni yaratish/yakunlash uchun.
@RestController
@RequestMapping("/log/v1/local")
@RequiredArgsConstructor
public class LocalController {

  private final PaymentReminderLogService paymentReminderLogService;

  // Cron boshlanganda: yangi audit yozuv yaratadi, entity qaytaradi (id finish uchun kerak).
  @PostMapping("/payment-reminder/start")
  public Mono<PaymentReminderLogEntity> startPaymentReminder() {
    return paymentReminderLogService.start();
  }

  // Cron tugaganda: id bo'yicha totalCandidates + sentSuccess + sentFailed + xato yozadi.
  @PostMapping("/payment-reminder/{id}/finish")
  public Mono<Void> finishPaymentReminder(
      @PathVariable UUID id, @RequestBody PaymentReminderFinishRequest req) {
    return paymentReminderLogService.finish(
        id, req.totalCandidates(), req.sentSuccess(), req.sentFailed(), req.errorMessage());
  }

  public record PaymentReminderFinishRequest(
      Integer totalCandidates, Integer sentSuccess, Integer sentFailed, String errorMessage) {}
}
