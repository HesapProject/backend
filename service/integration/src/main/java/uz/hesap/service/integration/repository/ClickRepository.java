package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.ClickEntity;

public interface ClickRepository extends R2dbcRepository<ClickEntity, UUID> {

  Mono<ClickEntity> findByClickTransId(final String clickTransId);

  Mono<ClickEntity> findByClickTransIdAndId(final String clickTransId, final UUID id);

  Mono<ClickEntity> findByClickTransIdAndMerchantTransId(
      String clickTransId, String merchantTransId);
}
