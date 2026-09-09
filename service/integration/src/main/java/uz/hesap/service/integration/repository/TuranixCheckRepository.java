package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.integration.domain.TuranixCheckEntity;

public interface TuranixCheckRepository
    extends ReactiveCrudRepository<TuranixCheckEntity, UUID> {

  Flux<TuranixCheckEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Flux<TuranixCheckEntity> findAllByMsisdnOrderByCreatedAtDesc(String msisdn, Pageable pageable);

  Flux<TuranixCheckEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
