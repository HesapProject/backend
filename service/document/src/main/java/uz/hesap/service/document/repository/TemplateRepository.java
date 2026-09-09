package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.domain.template.TemplateEntity;

public interface TemplateRepository extends R2dbcRepository<TemplateEntity, UUID> {
  // Ro'yxat/shartnoma yaratish tartibi: priority DESC (katta qiymat oldin),
  // teng bo'lsa eski yaratilgan oldin (createdDate ASC).
  Flux<TemplateEntity> findAllByDeletedFalseOrderByPriorityDescCreatedDateAsc();

  Flux<TemplateEntity> findAllByTemplateTypeAndDeletedFalseOrderByPriorityDescCreatedDateAsc(
      final TemplateType templateType);

  Mono<TemplateEntity> findByIdAndStatusAndTemplateTypeAndDeletedFalse(
      final UUID id, final TemplateStatus status, final TemplateType type);

  // templateId va type bo'yicha template olish (notice yoki report uchun)
  Mono<TemplateEntity> findTopByTemplateIdAndTemplateType(
      final UUID templateId, final TemplateType templateType);
}
