package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.UserPermissionEntity;

@Repository
public interface UserPermissionRepository extends R2dbcRepository<UserPermissionEntity, UUID> {

  // DIQQAT: property nomlari `userFromIn`/`userToIn` — derived query parser ulardagi
  // `In` qismini Spring Data'ning `In` operatori deb o'qiydi ("No property 'userFrom'...").
  // Shu sabab bu metodlar @Query (native SQL) bilan yoziladi.

  @Query(
      "SELECT * FROM \"user\".white_list WHERE user_from_in = :userFromIn"
          + " AND user_to_in = :userToIn AND deleted = false")
  Mono<UserPermissionEntity> findByUserFromInAndUserToInAndDeletedIsFalse(
      @Param("userFromIn") String userFromIn, @Param("userToIn") String userToIn);

  @Query(
      "SELECT * FROM \"user\".white_list WHERE user_from_in = :userFromIn"
          + " AND deleted = false AND active = true")
  Flux<UserPermissionEntity> findByUserFromInAndDeletedIsFalseAndActiveIsTrue(
      @Param("userFromIn") String userFromIn);

  @Query(
      "SELECT * FROM \"user\".white_list WHERE user_to_in = :userToIn"
          + " AND deleted = false AND active = true")
  Flux<UserPermissionEntity> findByUserToInAndDeletedIsFalseAndActiveIsTrue(
      @Param("userToIn") String userToIn);

  @Modifying
  @Query(
      "UPDATE \"user\".white_list SET active = false, passport = false, payability = false, "
          + "contract = false, partner = false, last_modified_date = NOW() "
          + "WHERE active = true AND (EXTRACT(EPOCH FROM NOW()) - EXTRACT(EPOCH FROM last_modified_date)) > 86400")
  Mono<Long> expireAllPermissions();
}
