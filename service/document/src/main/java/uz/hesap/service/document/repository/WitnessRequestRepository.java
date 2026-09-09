package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;

public interface WitnessRequestRepository
    extends ReactiveCrudRepository<WitnessRequestEntity, UUID> {
  Flux<WitnessRequestEntity> findAllByContractId(UUID contractId);

  // Menga kelgan guvohlik so'rovlari (men guvohman) — status bo'yicha (PENDING).
  Flux<WitnessRequestEntity> findAllByWitnessIdAndStatus(
      UUID witnessId, DocumentWitnessStatus status);

  Flux<WitnessRequestEntity> findAllByContractIdInAndStatus(
      List<UUID> contractIds, DocumentWitnessStatus status);

  Mono<WitnessRequestEntity> findByContractIdAndWitnessId(UUID contractId, UUID witnessId);

  Flux<WitnessRequestEntity> findAllByContractIdIn(List<UUID> contractIds);
}
