package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.template.ClaimTemplateEntity;

public interface ClaimTemplateRepository extends R2dbcRepository<ClaimTemplateEntity, UUID> {
  Flux<ClaimTemplateEntity> findAllByDeletedFalse();

  Mono<ClaimTemplateEntity> findByIdAndDeletedFalse(final UUID id);

  // Ota shartnoma shabloni bo'yicha da'vo arizasi shablonini topish (ClaimsService uchun).
  Mono<ClaimTemplateEntity> findFirstByContractTemplateIdAndDeletedFalse(
      final UUID contractTemplateId);
}
