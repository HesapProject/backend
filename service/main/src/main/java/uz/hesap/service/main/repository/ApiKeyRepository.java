package uz.hesap.service.main.repository;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.ApiKeyEntity;

@Repository
public interface ApiKeyRepository extends R2dbcRepository<ApiKeyEntity, UUID> {

  Mono<ApiKeyEntity> findByKeyHashAndDeletedFalse(String keyHash);

  Mono<ApiKeyEntity> findByIdAndDeletedFalse(UUID id);

  // DIQQAT: property nomi `ownerIn` — derived query parser undagi `In`ni Spring Data'ning
  // IN operatori deb o'qiydi (UserPackageRepository'dagi kabi), shu sabab @Query.
  @Query(
      "SELECT * FROM \"user\".api_key WHERE owner_in = :ownerIn AND deleted = false"
          + " ORDER BY created_date DESC")
  Flux<ApiKeyEntity> findAllByOwner(@Param("ownerIn") String ownerIn);

  // Webhook yetkazish uchun: taraflarning aktiv, muddati o'tmagan, URL'i bor kalitlari.
  @Query(
      "SELECT * FROM \"user\".api_key WHERE owner_in = ANY(:ins) AND deleted = false"
          + " AND active = true AND webhook_url IS NOT NULL"
          + " AND (expires_at IS NULL OR expires_at > now())")
  Flux<ApiKeyEntity> findWebhookTargets(@Param("ins") String[] ins);

  // Admin ro'yxati — barcha kalitlar (owner_in bo'yicha ixtiyoriy qidiruv).
  @Query(
      "SELECT * FROM \"user\".api_key WHERE deleted = false"
          + " AND (:search IS NULL OR owner_in ILIKE '%' || :search || '%'"
          + "      OR name ILIKE '%' || :search || '%')"
          + " ORDER BY created_date DESC LIMIT :limit OFFSET :offset")
  Flux<ApiKeyEntity> findAllAdmin(
      @Param("search") String search, @Param("limit") int limit, @Param("offset") long offset);

  @Query(
      "SELECT COUNT(*) FROM \"user\".api_key WHERE deleted = false"
          + " AND (:search IS NULL OR owner_in ILIKE '%' || :search || '%'"
          + "      OR name ILIKE '%' || :search || '%')")
  Mono<Long> countAllAdmin(@Param("search") String search);

  @Modifying
  @Query("UPDATE \"user\".api_key SET last_used_at = now() WHERE id = :id")
  Mono<Void> touchLastUsed(@Param("id") UUID id);

  // Pageable/Collection'ni @Query parametrlariga o'giruvchi qulaylik metodlari
  // (nomi boshqacha — derived-query parser overload'ni tahlil qilishga urinmasin).
  default Flux<ApiKeyEntity> findPageAdmin(String search, Pageable pageable) {
    return findAllAdmin(search, pageable.getPageSize(), pageable.getOffset());
  }

  default Flux<ApiKeyEntity> findWebhookTargetsFor(Collection<String> ins) {
    return findWebhookTargets(ins.toArray(new String[0]));
  }
}
