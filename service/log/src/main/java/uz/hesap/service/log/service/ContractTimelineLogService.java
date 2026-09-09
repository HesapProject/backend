package uz.hesap.service.log.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ContractTimelineLog;
import uz.hesap.service.log.domain.ContractTimelineLogEntity;
import uz.hesap.service.log.repository.ContractTimelineLogRepository;

@Service
@RequiredArgsConstructor
public class ContractTimelineLogService {

  private final ContractTimelineLogRepository repository;

  public Mono<Void> saveLog(ContractTimelineLog log) {
    ContractTimelineLogEntity entity = new ContractTimelineLogEntity();
    entity.setContractId(log.contractId());
    entity.setEventType(log.eventType());
    entity.setRole(log.role());
    entity.setActorIn(log.actorIn());
    entity.setAmount(log.amount());
    entity.setCurrency(log.currency());
    entity.setOccurredAt(log.occurredAt());
    entity.setSessionId(log.sessionId());
    entity.setDevice(log.device());
    return repository.save(entity).then();
  }

  // Shartnoma timeline'i — vaqt bo'yicha tartiblangan hodisalar.
  public Mono<List<ContractTimelineLogEntity>> getTimeline(UUID contractId) {
    return repository.findAllByContractIdOrderByOccurredAtAsc(contractId).collectList();
  }
}
