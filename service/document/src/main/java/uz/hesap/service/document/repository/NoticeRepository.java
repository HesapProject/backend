package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.NoticeEntity;
import uz.hesap.service.document.domain.enums.NoticeStatus;

public interface NoticeRepository
    extends ReactiveCrudRepository<NoticeEntity, UUID>, CustomNoticeRepository {

  Flux<NoticeEntity> findByContractIdAndDeletedFalse(UUID contractId, Pageable pageable);

  Mono<Long> countByContractIdAndDeletedFalse(UUID contractId);

  // status bo'yicha filtrlab sanash (report uchun kamida 2 ta CREATED talabnoma kerak)
  Mono<Long> countByContractIdAndStatusAndDeletedFalse(UUID contractId, NoticeStatus status);

  Mono<Long> countByDeletedFalse();
}
