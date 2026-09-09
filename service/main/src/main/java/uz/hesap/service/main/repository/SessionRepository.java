package uz.hesap.service.main.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.SessionEntity;

@Repository
public interface SessionRepository extends R2dbcRepository<SessionEntity, UUID> {

  // Auth: token'dagi sessionId (== deviceId) + userId bo'yicha sessiya tekshiruvi.
  Mono<SessionEntity> findByIdAndUserId(final UUID id, final UUID userId);

  // Bir qurilma (uuid) uchun BARCHA sessiyalarni arxivlaydi — logout (person + company).
  @Modifying
  @Query("UPDATE \"user\".session SET archived = true WHERE uuid = :uuid")
  Mono<Long> archiveByUuid(final String uuid);

  @Modifying
  @Query("UPDATE \"user\".session SET archived = true WHERE id = :sessionId AND user_id = :userId")
  Mono<Long> archiveByIdAndUserId(final UUID sessionId, final UUID userId);

  @Modifying
  @Query("UPDATE \"user\".session SET archived = true WHERE id = :sessionId")
  Mono<Long> archiveById(final UUID sessionId);

  // Login paytida shu qurilmadagi (uuid) eski sessiyalarni tozalaydi.
  Mono<Void> deleteByUuid(final String uuid);

  // Company token: shu (company, qurilma) uchun eski sessiyalarni tozalaydi.
  Mono<Void> deleteByUserIdAndUuid(final UUID userId, final String uuid);

  Mono<Void> deleteByIdAndUserId(final UUID sessionId, final UUID userId);

  Mono<Void> deleteByUserId(UUID userId);

  // Verify (ro'yxatdan o'tish) sessiyalari.
  Mono<SessionEntity> findByIdAndIsVerifyDeviceTrue(final UUID id);

  Mono<SessionEntity> findByPhoneAndIsVerifyDeviceTrue(final String phone);

  // Push: foydalanuvchining aktiv sessiyalaridagi FCM tokenlar.
  @Query(
      "SELECT fcm_token FROM \"user\".session "
          + "WHERE user_id = :userId AND archived = false AND fcm_token IS NOT NULL")
  Flux<String> findFcmTokensByUserId(final UUID userId);

  @Query(
      "SELECT fcm_token FROM \"user\".session "
          + "WHERE user_id = ANY(:userIds) AND archived = false AND fcm_token IS NOT NULL")
  Flux<String> findFcmTokensByUserIds(final UUID[] userIds);

  // Sessiyalar ro'yxati (aktiv).
  Flux<SessionEntity> findAllByUserIdAndArchivedFalse(UUID userId);

  Flux<SessionEntity> findAllByUserIdIn(List<UUID> userIds);

  Flux<SessionEntity> findAllByUserId(UUID userId);
}
