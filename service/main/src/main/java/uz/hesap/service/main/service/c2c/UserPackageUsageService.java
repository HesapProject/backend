package uz.hesap.service.main.service.c2c;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.TariffTemplateResponse;
import uz.hesap.service.common.util.TemplateConfigResponse;
import uz.hesap.service.common.util.enums.TemplateDistributionType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.domain.UserPackageEntity;
import uz.hesap.service.main.domain.UserPackageUsageEntity;
import uz.hesap.service.main.domain.enums.UserPackageUsageStatus;
import uz.hesap.service.main.repository.UserPackageRepository;
import uz.hesap.service.main.repository.UserPackageUsageRepository;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.PackageService;

// Paketdan foydalanish jurnali: har tuzilgan shartnoma uchun bitta ACTIVE qator.
@Service
@RequiredArgsConstructor
@Log4j2
public class UserPackageUsageService {

  private final UserPackageUsageRepository usageRepository;
  private final UserPackageRepository userPackageRepository;
  private final UserRepository userRepository;
  private final PackageService packageService;

  // Shartnoma tuzilganda chaqiriladi: shu template'ni beruvchi va qolgan soni bor aktiv
  // user_package'ni topib bitta ACTIVE foydalanish qatori qo'shadi. Mos paket yo'q — skip.
  // userPackageId berilsa — o'sha paket ishlatiladi (grant+qolgan tekshiriladi), aks holda
  // avtomatik tanlanadi. userIn (PINFL/STIR) userId'dan resolish qilinadi.
  public Mono<Void> recordUsage(
      UUID userId, UUID userPackageId, UUID templateId, UUID contractId) {
    if (userId == null || templateId == null || contractId == null) {
      return Mono.empty();
    }
    // Idempotent: shu shartnoma uchun ACTIVE usage allaqachon bo'lsa — qayta yozmaymiz
    // (paketdan 2 marta yechilishining oldini oladi: create+sign, retry va h.k.).
    return usageRepository
        .findByContractIdAndStatus(contractId, UserPackageUsageStatus.ACTIVE)
        .hasElement()
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                return Mono.empty();
              }
              return doRecordUsage(userId, userPackageId, templateId, contractId);
            });
  }

  private Mono<Void> doRecordUsage(
      UUID userId, UUID userPackageId, UUID templateId, UUID contractId) {
    return userRepository
        .findByIdAndDeletedIsFalse(userId)
        .map(UserEntity::getIn)
        .defaultIfEmpty("")
        .flatMap(
            userIn -> {
              if (userPackageId != null) {
                return userPackageRepository
                    .findByIdAndDeletedFalse(userPackageId)
                    .filterWhen(up -> hasRemaining(up, templateId))
                    .flatMap(up -> insert(userIn, up.getId(), templateId, contractId))
                    .switchIfEmpty(autoPick(userIn, templateId, contractId));
              }
              return autoPick(userIn, templateId, contractId);
            });
  }

  // Eng yangi aktiv paket — shu template'ni beruvchi va qolgan soni bor.
  private Mono<Void> autoPick(String userIn, UUID templateId, UUID contractId) {
    return userPackageRepository
        .findAllByUserInAndDeletedFalseAndExpDateAfterOrderByCreatedAtDesc(userIn, Instant.now())
        .concatMap(up -> hasRemaining(up, templateId).map(ok -> entry(up, ok)))
        .filter(Map.Entry::getValue)
        .next()
        .flatMap(e -> insert(userIn, e.getKey().getId(), templateId, contractId))
        .then();
  }

  // Shartnoma bekor qilinganda: shu shartnoma uchun ACTIVE usage'ni CANCELLED qiladi (son qaytadi).
  public Mono<Void> cancelUsage(UUID contractId) {
    return usageRepository
        .findByContractIdAndStatus(contractId, UserPackageUsageStatus.ACTIVE)
        .flatMap(
            u -> {
              u.setStatus(UserPackageUsageStatus.CANCELLED);
              return usageRepository.save(u);
            })
        .then();
  }

  // Display uchun: bitta user_package + template bo'yicha ishlatilgan (ACTIVE) soni.
  public Mono<Long> usedCount(UUID userPackageId, UUID templateId, boolean shared) {
    return shared
        ? usageRepository.countByUserPackageIdAndStatus(
            userPackageId, UserPackageUsageStatus.ACTIVE)
        : usageRepository.countByUserPackageIdAndTemplateIdAndStatus(
            userPackageId, templateId, UserPackageUsageStatus.ACTIVE);
  }

  // Paket shu template'ni beradimi va qolgan soni bormi.
  private Mono<Boolean> hasRemaining(UserPackageEntity up, UUID templateId) {
    return packageService
        .getById(up.getPackageId())
        .flatMap(
            pkg -> {
              TemplateConfigResponse config = pkg.templateConfig();
              if (config == null) {
                return Mono.just(false);
              }
              boolean shared = config.type() == TemplateDistributionType.SHARED;
              int granted = grantedFor(config, templateId, shared);
              if (granted <= 0) {
                return Mono.just(false); // bu paket bu template'ni bermaydi
              }
              return usedCount(up.getId(), templateId, shared).map(used -> used < granted);
            })
        .defaultIfEmpty(false);
  }

  // Paket config'idan template uchun berilgan son: SHARED → totalCount, aks holda template count.
  private int grantedFor(TemplateConfigResponse config, UUID templateId, boolean shared) {
    if (config.templates() == null) {
      return 0;
    }
    boolean contains =
        config.templates().stream()
            .anyMatch(t -> t.template() != null && templateId.equals(t.template().id()));
    if (!contains) {
      return 0;
    }
    if (shared) {
      return config.totalCount() != null ? config.totalCount() : 0;
    }
    return config.templates().stream()
        .filter(t -> t.template() != null && templateId.equals(t.template().id()))
        .map(TariffTemplateResponse::count)
        .filter(c -> c != null)
        .findFirst()
        .orElse(0);
  }

  private Mono<Void> insert(String userIn, UUID userPackageId, UUID templateId, UUID contractId) {
    UserPackageUsageEntity e = new UserPackageUsageEntity();
    e.setUserIn(userIn);
    e.setUserPackageId(userPackageId);
    e.setTemplateId(templateId);
    e.setContractId(contractId);
    e.setStatus(UserPackageUsageStatus.ACTIVE);
    return usageRepository.save(e).then();
  }

  private Map.Entry<UserPackageEntity, Boolean> entry(UserPackageEntity up, Boolean ok) {
    return new AbstractMap.SimpleEntry<>(up, ok);
  }
}
