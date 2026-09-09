package uz.hesap.service.document.service.document;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.webclient.UserServiceClient;

/**
 * Document bilan bog'liq umumiy yordamchi methodlar. Barcha servislar (NoticeService,
 * ClaimsService, PaymentScheduleService, ...) duplikat code o'rniga shu componentni ishlatadi.
 */
@Component
@RequiredArgsConstructor
public class DocumentQueryHelper {

  private final DocumentRepository documentRepository;
  private final UserServiceClient userServiceClient;

  /** Documentni ID bo'yicha topish — topilmasa NotFoundException. */
  public Mono<DocumentEntity> findOrThrow(UUID documentId) {
    return documentRepository
        .findByIdAndDeletedFalse(documentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Hujjat topilmadi: " + documentId)));
  }

  /**
   * Documentni topish + userId ga tegishli ekanligini tekshirish. userId buyer yoki seller bo'lishi
   * kerak, aks holda ForbiddenException.
   */
  public Mono<DocumentEntity> findAndValidateParty(UUID documentId, UUID userId) {
    return findOrThrow(documentId)
        .flatMap(
            document ->
                // Taraflar faqat PINFL'da — joriy user PINFL'ini (JWT'dagi) hujjatning
                // buyerIn/sellerIn'i bilan solishtiramiz.
                userServiceClient
                    .getUserById(userId)
                    .flatMap(
                        me -> {
                          final String myPinfl = me.in();
                          if (myPinfl != null
                              && (myPinfl.equals(document.getBuyerIn())
                                  || myPinfl.equals(document.getSellerIn()))) {
                            return Mono.just(document);
                          }
                          return forbiddenParty();
                        })
                    .switchIfEmpty(forbiddenParty()));
  }

  private Mono<DocumentEntity> forbiddenParty() {
    return Mono.error(
        new ForbiddenException("Faqat shartnoma ishtirokchisi bu amalni bajarishi mumkin"));
  }

  // ================ Taraf UUID resolish (notification recipient uchun) ================

  // PINFL/STIR'dan user UUID — topilmasa Mono.empty (notification flowni to'xtatmaslik uchun).
  // getUserByIn 404/xato bersa ham empty — migratsiya shartnomalarida qarama-qarshi taraf
  // ba'zan ilovada topilmaydi, bu asosiy amalni (masalan talabnoma yaratish) buzmasligi kerak.
  public Mono<UUID> resolveUserIdByIn(String in) {
    if (in == null || in.isBlank()) return Mono.empty();
    return userServiceClient.getUserByIn(in).map(u -> u.id()).onErrorResume(e -> Mono.empty());
  }

  // userId taraf sifatida buyer (true) yoki seller (false)? PINFL bo'yicha aniqlanadi.
  public Mono<Boolean> isBuyer(DocumentEntity document, UUID userId) {
    return userServiceClient
        .getUserById(userId)
        .map(me -> me.in() != null && me.in().equals(document.getBuyerIn()));
  }

  // Joriy user (userId) qarama-qarshi tarafining UUID'sini qaytaradi (recipient).
  // userId PINFL'i buyerIn'ga teng bo'lsa → sellerIn egasining UUID'si, aks holda buyerIn'niki.
  public Mono<UUID> resolveOppositePartyUserId(DocumentEntity document, UUID userId) {
    return userServiceClient
        .getUserById(userId)
        .map(me -> me.in())
        .flatMap(
            myPinfl -> {
              String otherIn =
                  myPinfl != null && myPinfl.equals(document.getBuyerIn())
                      ? document.getSellerIn()
                      : document.getBuyerIn();
              return resolveUserIdByIn(otherIn);
            });
  }
}
