package uz.hesap.service.main.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.repository.HesapScoreSignalsRepository;
import uz.hesap.service.main.service.c2c.HesapScoreService;

// Hesap Score kunlik qayta hisoblash — faqat faol shartnomalarda taraf bo'lganlar
// (uxlab yotgan userlar skorlanmaydi). O'zbekiston vaqti bilan soat 03:00.
@Log4j2
@Component
@RequiredArgsConstructor
public class HesapScoreCronService {

  private final HesapScoreSignalsRepository signalsRepository;
  private final HesapScoreService hesapScoreService;

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Tashkent")
  public void recomputeActiveParties() {
    log.info("Hesap Score kunlik qayta hisoblash (03:00 Asia/Tashkent) boshlandi...");
    signalsRepository
        .activePartyIdentifiers()
        .flatMap(
            in ->
                hesapScoreService
                    .computeAndStore(in)
                    .onErrorResume(
                        e -> {
                          log.warn("Hesap Score hisob xatosi ({}): {}", in, e.toString());
                          return Mono.empty();
                        }),
            4) // bir vaqtda 4 ta — DB'ni bosmaslik uchun
        .count()
        .subscribe(
            n -> log.info("Hesap Score qayta hisoblash tugadi: {} taraf", n),
            e -> log.error("Hesap Score cron xatosi", e));
  }
}
