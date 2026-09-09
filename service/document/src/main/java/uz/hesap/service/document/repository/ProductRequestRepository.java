package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;

public interface ProductRequestRepository
    extends ReactiveCrudRepository<ProductRequestEntity, UUID>, CustomProductRequestRepository {

  Mono<ProductRequestEntity> findByIdAndDeletedFalse(UUID id);

  Flux<ProductRequestEntity> findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(
      UUID contractId);

  // Shu mahsulot bo'yicha kutilayotgan (PENDING) so'rov bor-yo'qligini tekshirish uchun —
  // javob kelmaguncha yangi so'rov yuborilmasin.
  Flux<ProductRequestEntity> findAllByProductIdAndStatusAndDeletedFalse(
      UUID productId, ProductRequestStatus status);
}
