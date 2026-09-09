package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.log.domain.ContractTimelineLogEntity;

@Repository
public interface ContractTimelineLogRepository
    extends R2dbcRepository<ContractTimelineLogEntity, UUID> {
  Flux<ContractTimelineLogEntity> findAllByContractIdOrderByOccurredAtAsc(UUID contractId);
}
