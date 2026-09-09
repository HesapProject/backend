package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.SmsInviteTemplateEntity;

public interface SmsInviteTemplateRepository
    extends ReactiveCrudRepository<SmsInviteTemplateEntity, UUID> {

  Mono<SmsInviteTemplateEntity> findByLanguage(String language);
}
