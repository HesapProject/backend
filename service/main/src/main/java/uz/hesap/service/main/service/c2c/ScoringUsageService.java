package uz.hesap.service.main.service.c2c;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.ScoringUsageEntity;
import uz.hesap.service.main.model.ScoringUsageRequest;
import uz.hesap.service.main.repository.ScoringUsageRepository;

// Scoring ishlatilishini qayd etadi (har scoring paketning per-tur kvotasidan).
@Service
@RequiredArgsConstructor
public class ScoringUsageService {

  private final ScoringUsageRepository repository;

  public Mono<Void> record(ScoringUsageRequest req) {
    if (req.scoringType() == null) {
      return Mono.empty();
    }
    ScoringUsageEntity e = new ScoringUsageEntity();
    e.setUserIn(req.userIn());
    e.setPackageId(req.packageId());
    e.setScoringType(req.scoringType());
    e.setScoringRef(req.scoringRef());
    return repository.save(e).then();
  }

  // Paket bo'yicha shu turdagi scoring ishlatilgan soni.
  public Mono<Long> usedCount(java.util.UUID packageId, String scoringType) {
    if (packageId == null) return Mono.just(0L);
    return repository.countByPackageIdAndScoringType(packageId, scoringType);
  }
}
