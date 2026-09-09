package uz.hesap.service.document.service.c2c;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.model.enums.ActionType;

/** WitnessController doirasi — guvoh tasdiqlash (SMS, MyID) / rad. */
@Service
@RequiredArgsConstructor
public class WitnessService {

  private final C2CDocumentActionService actionDocumentService;

  public Mono<Void> sendSms(UUID docId, UserResponse user) {
    return actionDocumentService.sendVerificationSms(docId, ActionType.WITNESS_ACCEPT, user);
  }

  public Mono<Void> acceptWitness(UUID docId, UUID userId, Integer code) {
    return actionDocumentService.acceptWitness(docId, userId, code);
  }

  public Mono<Void> signWitnessByMyId(UUID docId, UUID userId, String code, String platform) {
    return actionDocumentService.signWitnessByMyId(docId, userId, code, platform);
  }

  public Mono<Void> signWitnessByAbleId(UUID docId, UUID userId, String attemptId) {
    return actionDocumentService.signWitnessByAbleId(docId, userId, attemptId);
  }

  public Mono<Void> acceptWitnessSimple(UUID docId, UUID userId) {
    return actionDocumentService.acceptWitnessSimple(docId, userId);
  }

  public Mono<Void> rejectWitness(UUID docId, UUID userId) {
    return actionDocumentService.rejectWitness(docId, userId);
  }
}
