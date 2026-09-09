package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.NotificationEntity;
import uz.hesap.service.integration.util.Constants;

@Repository
public interface NotificationRepository extends R2dbcRepository<NotificationEntity, UUID> {
  Flux<NotificationEntity> findByUserIdAndDeletedIsFalse(UUID userId, Pageable pageable);

  Mono<Long> countByUserIdAndDeletedIsFalse(UUID userId);

  Mono<NotificationEntity> findByIdAndUserIdAndDeletedIsFalse(UUID id, UUID userId);

  @Query(
      "update "
          + Constants.SCHEMA
          + "."
          + Constants.TABLE_NOTIFICATION
          + " set deleted = true where data_id = :dataId ")
  Mono<Void> deleteNotificationByDataId(UUID dataId);

  // Foydalanuvchining barcha o'qilmagan bildirishnomalarini o'qilgan qiladi.
  @Query(
      "update "
          + Constants.SCHEMA
          + "."
          + Constants.TABLE_NOTIFICATION
          + " set is_viewed = true where user_id = :userId and deleted = false"
          + " and is_viewed = false ")
  Mono<Void> markAllViewedByUserId(UUID userId);
}
