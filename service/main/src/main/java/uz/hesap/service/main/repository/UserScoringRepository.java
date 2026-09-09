package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import uz.hesap.service.main.domain.UserScoringEntity;

public interface UserScoringRepository extends R2dbcRepository<UserScoringEntity, UUID> {
  // Bitta odamning barcha scoringlari (jamlangan) — eng yangi birinchi.
  // @Query — `userIn` derived query "UserIn" ni `user`+In-keyword deb o'qib startup
  // crash beradi (In-keyword tuzog'i). Shuning uchun aniq SQL + @Param.
  @Query("SELECT * FROM \"user\".user_scoring WHERE user_in = :userIn ORDER BY created_at DESC")
  Flux<UserScoringEntity> findByUser(@Param("userIn") String userIn);

  // So'rovchi (requester) shu odamni (userIn) qilgan scoringlari — eng yangi birinchi.
  @Query(
      "SELECT * FROM \"user\".user_scoring "
          + "WHERE requester_in = :requesterIn AND user_in = :userIn "
          + "ORDER BY created_at DESC")
  Flux<UserScoringEntity> findByRequesterAndUser(
      @Param("requesterIn") String requesterIn, @Param("userIn") String userIn);
}
