package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.CurrencyEntity;

public interface CurrencyRepository extends R2dbcRepository<CurrencyEntity, UUID> {

  Flux<CurrencyEntity> findAllByDeletedFalseOrderBySortOrderAsc();

  Flux<CurrencyEntity> findAllByDeletedFalseAndIsActiveTrueOrderBySortOrderAsc();

  Mono<CurrencyEntity> findByIdAndDeletedFalse(UUID id);

  Mono<CurrencyEntity> findByCodeAndDeletedFalse(String code);
}
