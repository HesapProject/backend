package uz.hesap.service.main.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.UserPackageUsageEntity;
import uz.hesap.service.main.domain.enums.UserPackageUsageStatus;

@Repository
public interface UserPackageUsageRepository
    extends R2dbcRepository<UserPackageUsageEntity, UUID> {

  // bitta user_package + template bo'yicha ACTIVE foydalanishlar soni (ishlatilgan).
  Mono<Long> countByUserPackageIdAndTemplateIdAndStatus(
      UUID userPackageId, UUID templateId, UserPackageUsageStatus status);

  // bitta user_package bo'yicha barcha ACTIVE foydalanishlar soni (SHARED pool uchun).
  Mono<Long> countByUserPackageIdAndStatus(UUID userPackageId, UserPackageUsageStatus status);

  // user_package bo'yicha barcha foydalanish qatorlari.
  Flux<UserPackageUsageEntity> findAllByUserPackageId(UUID userPackageId);

  // batch: bir nechta user_package bo'yicha ACTIVE qatorlar.
  Flux<UserPackageUsageEntity> findAllByUserPackageIdInAndStatus(
      List<UUID> userPackageIds, UserPackageUsageStatus status);

  // shartnoma bekor qilinganda usage qatorini topish.
  Mono<UserPackageUsageEntity> findByContractIdAndStatus(
      UUID contractId, UserPackageUsageStatus status);
}
