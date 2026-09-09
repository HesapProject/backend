package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.DocumentWitnessEntity;

public interface DocumentWitnessRepository
    extends ReactiveCrudRepository<DocumentWitnessEntity, UUID> {
  Flux<DocumentWitnessEntity> findAllByContractId(UUID documentId);

  Mono<DocumentWitnessEntity> findByContractIdAndWitnessId(UUID documentId, final UUID witnessId);

  Flux<DocumentWitnessEntity> findAllByContractIdIn(List<UUID> documentId);
}
