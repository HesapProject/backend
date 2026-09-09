package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.PackageEntity;
import uz.hesap.service.common.util.enums.TariffType;

@Repository
public interface PackageRepository extends R2dbcRepository<PackageEntity, UUID> {

  Mono<PackageEntity> findByIdAndDeletedFalse(UUID id);

  Flux<PackageEntity> findAllByDeletedFalseOrderByCreatedDateDesc();

  Flux<PackageEntity> findAllByDeletedFalseOrderByCreatedDateDesc(Pageable pageable);

  Mono<Long> countByDeletedFalse();

  // type filter
  Flux<PackageEntity> findAllByTypeAndDeletedFalseOrderByCreatedDateDesc(TariffType type);

  Flux<PackageEntity> findAllByTypeAndDeletedFalseOrderByCreatedDateDesc(
      TariffType type, Pageable pageable);

  Mono<Long> countByTypeAndDeletedFalse(TariffType type);
}
