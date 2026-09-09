package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.UserCardEntity;

@Repository
public interface UserCardRepository extends ReactiveCrudRepository<UserCardEntity, UUID> {

  // DIQQAT: `user_in` (PINFL) bo'yicha derived query YOZMA — Spring Data 'In'ni IN-keyword
  // deb o'qiydi (user IN ...) → startup crash. Shuning uchun @Query.
  @Query("SELECT * FROM billing.plum_cards WHERE user_in = :userIn")
  Flux<UserCardEntity> findByUserIn(String userIn);

  // Plum karta id (Long) bo'yicha — confirmPayment javobida cardId keladi.
  Mono<UserCardEntity> findByCardId(Long cardId);
}
