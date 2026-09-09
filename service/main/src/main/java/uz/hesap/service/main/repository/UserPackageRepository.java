package uz.hesap.service.main.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.UserPackageEntity;

@Repository
public interface UserPackageRepository extends R2dbcRepository<UserPackageEntity, UUID> {

  // DIQQAT: property `userIn` — derived query parser undagi `In`ni Spring Data'ning IN
  // operatori deb o'qiydi ("No property 'user'...") → startup crash. Shu sabab @Query.

  @Query(
      "SELECT * FROM \"user\".user_package WHERE user_in = :userIn AND deleted = false"
          + " ORDER BY created_at DESC")
  Flux<UserPackageEntity> findAllByUserInAndDeletedFalseOrderByCreatedAtDesc(
      @Param("userIn") String userIn);

  // Aktiv (muddati o'tmagan) paketlar.
  @Query(
      "SELECT * FROM \"user\".user_package WHERE user_in = :userIn AND deleted = false"
          + " AND exp_date > :now ORDER BY created_at DESC")
  Flux<UserPackageEntity> findAllByUserInAndDeletedFalseAndExpDateAfterOrderByCreatedAtDesc(
      @Param("userIn") String userIn, @Param("now") Instant now);

  // Arxiv (muddati o'tgan) paketlar.
  @Query(
      "SELECT * FROM \"user\".user_package WHERE user_in = :userIn AND deleted = false"
          + " AND exp_date < :now ORDER BY created_at DESC")
  Flux<UserPackageEntity> findAllByUserInAndDeletedFalseAndExpDateBeforeOrderByCreatedAtDesc(
      @Param("userIn") String userIn, @Param("now") Instant now);

  Flux<UserPackageEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

  Mono<UserPackageEntity> findByIdAndDeletedFalse(UUID id);

  Mono<Long> countByDeletedFalse();

  @Query("SELECT COUNT(*) FROM \"user\".user_package WHERE user_in = :userIn AND deleted = false")
  Mono<Long> countByUserInAndDeletedFalse(@Param("userIn") String userIn);
}
