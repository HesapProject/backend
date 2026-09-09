package uz.hesap.service.log.api.v1;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.Activity;
import uz.hesap.service.log.model.ActivityLogResponse;
import uz.hesap.service.log.model.ClickLogResponse;
import uz.hesap.service.log.model.EskizLogResponse;
import uz.hesap.service.log.model.MyIdLogResponse;
import uz.hesap.service.log.model.OneIdLogResponse;
import uz.hesap.service.log.model.PaymeLogResponse;
import uz.hesap.service.log.model.PlumLogResponse;
import uz.hesap.service.log.model.TuranixLogResponse;
import uz.hesap.service.log.service.ClickLogService;
import uz.hesap.service.log.service.EskizLogService;
import uz.hesap.service.log.service.MyIdLogService;
import uz.hesap.service.log.service.OneIdLogService;
import uz.hesap.service.log.service.PaymeLogService;
import uz.hesap.service.log.service.PlumLogService;
import uz.hesap.service.log.service.ActivityLogService;
import uz.hesap.service.log.service.ContractTimelineLogService;
import uz.hesap.service.log.service.TuranixLogService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/log/v1/logs")
public class LogController {

  private final OneIdLogService logService;
  private final EskizLogService eskizLogService;
  private final MyIdLogService myIdLogService;
  private final PlumLogService plumLogService;
  private final PaymeLogService paymeLogService;
  private final ClickLogService clickLogService;
  private final TuranixLogService turanixLogService;
  private final uz.hesap.service.log.service.RoumingLogService roumingLogService;
  private final ContractTimelineLogService contractTimelineLogService;
  private final ActivityLogService activityLogService;
  private final uz.hesap.service.log.service.PaymentReminderLogService paymentReminderLogService;

  // Foydalanuvchi amallari (activity_log) — actorIn (PINFL/STIR)/companyIn/activity/sana filtr.
  @GetMapping("/activity")
  public Mono<Page<ActivityLogResponse>> getActivityLogs(
      @RequestParam(required = false) String actorIn,
      @RequestParam(required = false) String companyIn,
      @RequestParam(required = false) Activity activity,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return activityLogService.getActivityLogs(
        actorIn, companyIn, activity, startDate, endDate, pageable);
  }

  // Shartnoma timeline (Log tab) — RabbitMQ orqali yig'ilgan hodisalar, vaqt bo'yicha.
  @GetMapping("/contract-timeline/{contractId}")
  public Mono<java.util.List<uz.hesap.service.log.domain.ContractTimelineLogEntity>>
      getContractTimeline(@org.springframework.web.bind.annotation.PathVariable UUID contractId) {
    return contractTimelineLogService.getTimeline(contractId);
  }

  @GetMapping("/one-id")
  public Mono<Page<OneIdLogResponse>> getOneIdLogs(
      @RequestParam(required = false) UUID companyId,
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return logService.getOneIdLogs(companyId, userId, startDate, endDate, pageable);
  }

  @GetMapping("/my-id")
  public Mono<Page<MyIdLogResponse>> getMyIdLogs(
      @RequestParam(required = false) UUID companyId,
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return myIdLogService.getMyIdLogs(companyId, userId, startDate, endDate, pageable);
  }

  @GetMapping("/plum")
  public Mono<Page<PlumLogResponse>> getPlumLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) UUID cardId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return plumLogService.getPlumLogs(userId, cardId, startDate, endDate, pageable);
  }

  @GetMapping("/payme")
  public Mono<Page<PaymeLogResponse>> getPaymeLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) UUID companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return paymeLogService.getPaymeLogs(userId, companyId, startDate, endDate, pageable);
  }

  @GetMapping("/click")
  public Mono<Page<ClickLogResponse>> getClickLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) UUID companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return clickLogService.getClickLogs(userId, companyId, startDate, endDate, pageable);
  }

  @GetMapping("/turanix")
  public Mono<Page<TuranixLogResponse>> getTuranixLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String msisdn,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return turanixLogService.getTuranixLogs(userId, msisdn, startDate, endDate, pageable);
  }

  // Rouming (ЭСФ) draft yuborish loglari — monitoring (sana filtri ixtiyoriy).
  @GetMapping("/rouming")
  public Mono<Page<uz.hesap.service.log.model.RoumingLogResponse>> getRoumingLogs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant endDate,
      Pageable pageable) {
    return roumingLogService.getRoumingLogs(startDate, endDate, pageable);
  }

  @GetMapping("/eskiz")
  public Mono<Page<EskizLogResponse>> getEskizLogs(
      @RequestParam(required = false) Boolean isFailed,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant toDate,
      Pageable pageable) {
    return eskizLogService.getLogs(isFailed, fromDate, toDate, pageable);
  }

  // To'lov eslatmasi cron tarixi — PaymentCronService har kunlik run natijasi.
  @GetMapping("/payment-reminder")
  public Mono<Page<uz.hesap.service.log.model.PaymentReminderLogResponse>> getPaymentReminderLogs(
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "20") Integer size) {
    return paymentReminderLogService.getLogs(page, size);
  }
}
