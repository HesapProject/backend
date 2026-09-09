package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.payment.PaymentEntity;

public interface PaymentScheduleRepository extends R2dbcRepository<PaymentEntity, UUID> {
  Flux<PaymentEntity> findAllByContractIdAndDeletedFalse(final UUID contractId);

  Mono<PaymentEntity> findByIdAndDeletedFalse(final UUID id);

  // Shartnoma statusi o'zgarganda — o'sha shartnomaning barcha (o'chirilmagan) to'lov
  // jadvallaridagi snapshot (contract_status) ni yangilaydi. status — enum nomi (varchar).
  @Modifying
  @Query(
      "UPDATE document.payments SET contract_status = :status "
          + "WHERE contract_id = :contractId AND deleted = false")
  Mono<Long> updateContractStatusByContractId(
      @Param("contractId") final UUID contractId, @Param("status") final String status);
}
