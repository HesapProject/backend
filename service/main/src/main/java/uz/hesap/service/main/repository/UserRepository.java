package uz.hesap.service.main.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;

@Repository
public interface UserRepository extends R2dbcRepository<UserEntity, UUID>, CustomUserRepository {

  Mono<Boolean> existsByPhoneAndTypeAndDeletedFalse(final String phone, final UserType userType);

  Flux<UserEntity> findAllByIdIn(List<UUID> idList);

  // IN (pinfl) bo'yicha userlarni topish — white_list IN-asosli bo'lgani uchun.
  // `in` property + `In` operatori to'qnashuvidan qochish uchun @Query (native).
  @Query("SELECT * FROM \"user\".\"user\" WHERE pinfl IN (:inList) AND deleted = false")
  Flux<UserEntity> findAllByInIn(@Param("inList") List<String> inList);

  Mono<UserEntity> findByIdAndDeletedIsFalse(final UUID id);

  Mono<UserEntity> findByIdAndTypeAndDeletedIsFalse(final UUID id, final UserType type);

  // Admin login (ADMIN/SUPER_ADMIN) — login bo'yicha topish.
  Mono<UserEntity> findByLoginAndDeletedFalse(String login);

  // Control admin akkauntlari ro'yxati (ADMIN/SUPER_ADMIN), yangi tepada.
  @Query(
      "SELECT * FROM \"user\".\"user\" WHERE type IN ('ADMIN', 'SUPER_ADMIN')"
          + " AND deleted = false ORDER BY created_date DESC")
  Flux<UserEntity> findAdmins();

  Mono<UserEntity> findByPhoneAndDeletedFalseAndType(String phone, UserType type);

  // Phone verify flow: type'ga bog'lamasdan LIMIT 1 olish (duplicate phone bo'lsa ham).
  Mono<UserEntity> findFirstByPhoneAndDeletedFalseOrderByCreatedDateAsc(String phone);

  Mono<Boolean> existsByPhoneAndTypeAndDeletedFalseAndIdNot(String phone, UserType type, UUID id);

  Mono<Boolean> existsByPhoneAndDeletedFalseAndType(String phone, UserType type);

  // Username operations for client users
  Mono<Boolean> existsByUsernameAndTypeAndDeletedFalse(String username, UserType type);

  // in (PINFL/TIN) + type bo'yicha qidirish.
  // DIQQAT: property nomi `in` (ustun `pinfl`) Spring Data'ning `In` kalit so'zi
  // bilan to'qnashadi — derived query "Operator IN requires Collection" deb
  // startupda yiqiladi. Shu sabab @Query (native SQL) ishlatamiz.
  @Query("SELECT * FROM \"user\".\"user\" WHERE pinfl = :in AND type = :type AND deleted = false")
  Mono<UserEntity> findByInAndTypeAndDeletedFalse(@Param("in") String in, @Param("type") UserType type);

  // IN (PINFL) bo'yicha LIMIT 1 qidirish — ayni IN bilan duplicate CLIENT
  // qatorlar bo'lsa (test DB-da uchragan) eng eskisini qaytaradi.
  // OneID verify oqimi shuni ishlatadi: ko'p natija "returned non unique result"
  // 500 xatosini beradi, eng eskisini olish bilan flow tugaydi.
  @Query(
      "SELECT * FROM \"user\".\"user\" WHERE pinfl = :in AND type = :type AND deleted = false"
          + " ORDER BY created_date ASC LIMIT 1")
  Mono<UserEntity> findFirstByInAndTypeAndDeletedFalseOrderByCreatedDateAsc(
      @Param("in") String in, @Param("type") UserType type);

  // PINFL/IN bo'yicha (type'siz) eng eski yozuv — admin tab'lari pinfl→userId resolve uchun.
  @Query(
      "SELECT * FROM \"user\".\"user\" WHERE pinfl = :in AND deleted = false"
          + " ORDER BY created_date ASC LIMIT 1")
  Mono<UserEntity> findFirstByInAndDeletedFalseOrderByCreatedDateAsc(@Param("in") String in);

  // Faol (deleted=false) yozuv topilmaganda fallback — eng oxirgi yozuv, o'chirilgan
  // bo'lsa ham. Shartnoma tomoni akkauntini o'chirgan bo'lsa ham profilini ko'rsatish
  // uchun (aks holda /users/{in} 404 "topilmadi" berardi).
  @Query(
      "SELECT * FROM \"user\".\"user\" WHERE pinfl = :in"
          + " ORDER BY created_date DESC LIMIT 1")
  Mono<UserEntity> findFirstByInOrderByCreatedDateDesc(@Param("in") String in);

  // TIN (yuridik shaxs STIR) bo'yicha COMPANY userni qidiradi — OneID legal flow.
  // findFirstBy + Order — duplicate xavfsizligi uchun.
  Mono<UserEntity> findFirstByTinAndTypeAndDeletedFalseOrderByCreatedDateAsc(
      String tin, UserType type);
}
