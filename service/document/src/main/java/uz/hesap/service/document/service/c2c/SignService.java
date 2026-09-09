package uz.hesap.service.document.service.c2c;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.model.enums.ActionType;
import uz.hesap.service.document.service.eimzo.EImzoService;

/** SignController doirasi — taraf imzolash (E-IMZO, SMS, MyID) + bekor qilish. */
@Service
@RequiredArgsConstructor
public class SignService {

  private final C2CDocumentActionService actionDocumentService;
  private final EImzoService eImzoService;

  public Mono<String> getTimestamp(String pkcs7, ServerHttpRequest request) {
    return eImzoService.getTimestamp(pkcs7, request);
  }

  public Mono<Void> signDocument(String pkcs7, ServerHttpRequest request) {
    return eImzoService.signDocument(pkcs7, request);
  }

  public Mono<Void> sendSms(UUID docId, UserResponse user) {
    return actionDocumentService.sendVerificationSms(docId, ActionType.PARTY_ACCEPT, user);
  }

  public Mono<Void> acceptParty(UUID docId, UUID userId, Integer code, UUID userPackageId) {
    return actionDocumentService.acceptParty(docId, userId, code, userPackageId);
  }

  public Mono<Void> acceptPartySimple(UUID docId, UUID userId, UUID userPackageId) {
    return actionDocumentService.acceptPartySimple(docId, userId, userPackageId);
  }

  public Mono<Void> signPartyByMyId(
      UUID docId, UUID userId, String code, String platform, UUID userPackageId) {
    return actionDocumentService.signPartyByMyId(docId, userId, code, platform, userPackageId);
  }

  public Mono<Void> signPartyByAbleId(UUID docId, UUID userId, String attemptId, UUID userPackageId) {
    return actionDocumentService.signPartyByAbleId(docId, userId, attemptId, userPackageId);
  }

  public Mono<Void> rejectParty(UUID docId, UUID userId) {
    return actionDocumentService.rejectParty(docId, userId);
  }
}
