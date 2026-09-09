package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.BalanceEntity;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;

public interface BalanceRepository extends R2dbcRepository<BalanceEntity, UUID> {

  Mono<BalanceEntity> findByUniqueIdAndBalanceTypeAndDeletedFalse(
      UUID uniqueId, BalanceType balanceType);

  Mono<BalanceEntity> findByUniqueIdAndBalanceTypeAndBillingTypeAndDeletedFalse(
      UUID uniqueId, BalanceType balanceType, BillingType billingType);
}
