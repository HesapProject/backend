package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.ClaimEntity;
import uz.hesap.service.document.domain.enums.ClaimStatus;

public interface ClaimRepository
    extends ReactiveCrudRepository<ClaimEntity, UUID>, CustomClaimRepository {

  Flux<ClaimEntity> findByContractIdAndDeletedFalse(UUID documentId, Pageable pageable);

  Mono<Long> countByContractIdAndDeletedFalse(UUID documentId);

  // oxirgi CREATED statusli reportni olish
  Mono<ClaimEntity> findFirstByContractIdAndStatusAndDeletedFalseOrderByCreatedDateDesc(
      UUID documentId, ClaimStatus status);
}
