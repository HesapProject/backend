package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;

public interface PaidScheduleRepository extends R2dbcRepository<PaidScheduleEntity, UUID> {
  Flux<PaidScheduleEntity> findAllByDocumentIdAndDeletedFalse(final UUID documentId);

  Flux<PaidScheduleEntity> findAllByPaymentScheduleIdAndDeletedFalse(final UUID paymentScheduleId);

  Mono<PaidScheduleEntity> findByIdAndDeletedFalse(final UUID id);
}
