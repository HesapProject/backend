package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.main.domain.BannerEntity;

@Repository
public interface BannerRepository extends R2dbcRepository<BannerEntity, UUID> {
  Flux<BannerEntity> findAllByIsDeletedFalseOrderBySortOrderAsc();

  Flux<BannerEntity> findAllByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAsc();
}
