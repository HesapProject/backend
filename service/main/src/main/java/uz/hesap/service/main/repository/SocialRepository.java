package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.SocialEntity;

@Repository
public interface SocialRepository extends R2dbcRepository<SocialEntity, UUID> {

  // DIQQAT: `user_in` (PINFL) derived query YOZMA — Spring Data 'In'ni IN-keyword deb
  // o'qiydi (user IN ...) → startup crash. Shuning uchun @Query.
  @Query("SELECT * FROM \"user\".socials WHERE user_in = :userIn")
  Mono<SocialEntity> findByUserIn(String userIn);
}
