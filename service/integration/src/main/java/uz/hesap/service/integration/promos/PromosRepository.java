package uz.hesap.service.integration.promos;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PromosRepository extends R2dbcRepository<PromosEntity, UUID> {

  Mono<PromosEntity> findByIdAndDeletedFalse(UUID id);

  Mono<PromosEntity> findByCodeAndDeletedFalse(String code);

  Flux<PromosEntity> findAllByDeletedFalse();

  Flux<PromosEntity> findAllByDeletedFalse(Pageable pageable);

  Mono<Long> countByDeletedFalse();
}
