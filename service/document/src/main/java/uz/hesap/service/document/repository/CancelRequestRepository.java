package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.CancelRequestEntity;
import uz.hesap.service.document.domain.enums.CancelRequestStatus;

public interface CancelRequestRepository
    extends ReactiveCrudRepository<CancelRequestEntity, UUID>, CustomCancelRequestRepository {

  Mono<CancelRequestEntity> findByIdAndDeletedFalse(UUID id);

  Flux<CancelRequestEntity> findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(
      UUID contractId);

  // Bitta shartnomada bir vaqtda faqat bitta PENDING so'rov bo'lishi uchun tekshiruv.
  Mono<CancelRequestEntity> findFirstByContractIdAndStatusAndDeletedFalse(
      UUID contractId, CancelRequestStatus status);
}
