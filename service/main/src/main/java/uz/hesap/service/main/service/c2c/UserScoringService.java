package uz.hesap.service.main.service.c2c;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.UserScoringEntity;
import uz.hesap.service.main.model.UserScoringRequest;
import uz.hesap.service.main.repository.UserScoringRepository;

// Umumlashgan scoring jurnali — har scoring uchun bitta qator (usage/kvota EMAS).
// Bitta odamning barcha scoringlarini bir joyda jamlaydi.
@Service
@RequiredArgsConstructor
public class UserScoringService {

  private final UserScoringRepository repository;

  public Mono<Void> record(UserScoringRequest req) {
    UserScoringEntity e = new UserScoringEntity();
    e.setScoringId(req.scoringId());
    e.setScoringType(req.scoringType());
    e.setRequesterIn(req.requesterIn());
    e.setUserIn(req.userIn());
    return repository.save(e).then();
  }

  // Bitta odamning barcha scoringlari (jamlangan).
  public Flux<UserScoringEntity> byUser(String userIn) {
    return repository.findByUser(userIn);
  }

  // So'rovchi (requester) shu odamni (userIn) qilgan scoringlari — partner tarixi.
  public Flux<UserScoringEntity> byRequesterAndUser(String requesterIn, String userIn) {
    return repository.findByRequesterAndUser(requesterIn, userIn);
  }
}
