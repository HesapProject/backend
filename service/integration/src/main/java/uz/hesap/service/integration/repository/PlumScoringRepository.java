package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.integration.domain.PlumScoringEntity;
import uz.hesap.service.integration.domain.enums.ScoringStatus;

public interface PlumScoringRepository extends ReactiveCrudRepository<PlumScoringEntity, UUID> {

  // DIQQAT: `user_in` (PINFL) bo'yicha derived query YOZMA — Spring Data 'In'ni IN-keyword
  // deb o'qiydi (user IN ...) → startup crash. Shuning uchun @Query + qo'lda LIMIT/OFFSET.
  @Query(
      "SELECT * FROM integration.plum_scoring WHERE user_in = :userIn"
          + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PlumScoringEntity> findByUserIn(String userIn, int limit, long offset);

  @Query(
      "SELECT * FROM integration.plum_scoring WHERE user_in = :userIn AND card_id = :cardId"
          + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PlumScoringEntity> findByUserInAndCardId(String userIn, UUID cardId, int limit, long offset);

  @Query(
      "SELECT * FROM integration.plum_scoring WHERE requester_in = :requesterIn"
          + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PlumScoringEntity> findByRequesterIn(String requesterIn, int limit, long offset);

  @Query(
      "SELECT * FROM integration.plum_scoring WHERE requester_in = :requesterIn AND card_id = :cardId"
          + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<PlumScoringEntity> findByRequesterInAndCardId(String requesterIn, UUID cardId, int limit, long offset);

  Flux<PlumScoringEntity> findAllByStatus(ScoringStatus status);
}
