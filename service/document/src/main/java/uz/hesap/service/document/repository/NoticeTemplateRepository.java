package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.template.NoticeTemplateEntity;

public interface NoticeTemplateRepository extends R2dbcRepository<NoticeTemplateEntity, UUID> {
  Flux<NoticeTemplateEntity> findAllByDeletedFalse();

  Mono<NoticeTemplateEntity> findByIdAndDeletedFalse(final UUID id);

  // Ota shartnoma shabloni bo'yicha talabnoma shablonini topish (NoticeService uchun).
  Mono<NoticeTemplateEntity> findFirstByContractTemplateIdAndDeletedFalse(
      final UUID contractTemplateId);
}
