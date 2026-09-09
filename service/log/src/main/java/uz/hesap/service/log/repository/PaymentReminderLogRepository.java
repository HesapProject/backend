package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.PaymentReminderLogEntity;

public interface PaymentReminderLogRepository
    extends R2dbcRepository<PaymentReminderLogEntity, UUID> {

  Flux<PaymentReminderLogEntity> findAllByOrderByStartedAtDesc(final Pageable pageable);

  Mono<Long> count();
}
