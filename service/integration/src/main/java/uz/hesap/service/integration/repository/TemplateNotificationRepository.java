package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.integration.domain.TemplateNotificationEntity;

public interface TemplateNotificationRepository
    extends ReactiveCrudRepository<TemplateNotificationEntity, UUID> {

  Flux<TemplateNotificationEntity> findAllByTemplateIdAndDeletedFalse(UUID templateId);

  Mono<TemplateNotificationEntity> findByTemplateIdAndEventAndDeletedFalse(
      UUID templateId, NotificationEvent event);
}
