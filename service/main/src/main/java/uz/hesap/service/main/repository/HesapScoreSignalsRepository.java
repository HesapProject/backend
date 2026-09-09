package uz.hesap.service.main.repository;

import java.time.Instant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.HesapScoreSignals;

// Hesap Score xom signallarini document.* dan cross-schema o'qiydi (bitta hesap DB).
public interface HesapScoreSignalsRepository {

  // Bitta foydalanuvchi (PINFL/STIR) uchun barcha signal.
  Mono<HesapScoreSignals> getSignals(String userIn, Instant cutoff, double halfLifeDays);

  // Kunlik cron uchun — faol shartnomalarda taraf bo'lgan barcha PINFL/STIR.
  Flux<String> activePartyIdentifiers();
}
