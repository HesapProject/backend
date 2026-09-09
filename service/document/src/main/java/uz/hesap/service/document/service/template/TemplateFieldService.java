package uz.hesap.service.document.service.template;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.template.TemplateFieldEntity;
import uz.hesap.service.document.model.mapper.TemplateFieldMapper;
import uz.hesap.service.document.model.request.TemplateFieldRequest;
import uz.hesap.service.document.model.response.TemplateFieldResponse;
import uz.hesap.service.document.repository.TemplateFieldRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class TemplateFieldService {
  private final TemplateFieldRepository templateFieldRepository;

  public Flux<TemplateFieldResponse> getAll(final UUID templateId) {
    return templateFieldRepository
        .findAllByTemplateIdAndDeletedFalse(templateId)
        .map(TemplateFieldMapper.INSTANCE::toResponse);
  }

  @Transactional
  public Flux<TemplateFieldResponse> upsert(
      final UUID templateId, final List<TemplateFieldRequest> request) {
    if (request == null || request.isEmpty()) {
      return Flux.error(new IllegalArgumentException("request is empty"));
    }

    return Flux.fromIterable(request)
        .concatMap(parentReq -> upsertField(templateId, parentReq))
        .map(TemplateFieldEntity::getId)
        .collectList()
        .flatMap(
            keepIds ->
                templateFieldRepository
                    .softDeleteNotIn(templateId, keepIds.toArray(new UUID[0]))
                    .then())
        .thenMany(loadResponse(templateId));
  }

  private Mono<TemplateFieldEntity> upsertField(UUID templateId, TemplateFieldRequest req) {
    if (req.id() == null) {
      TemplateFieldEntity e = TemplateFieldMapper.INSTANCE.toEntity(req);
      e.setTemplateId(templateId);
      e.setDeleted(Boolean.FALSE);
      return templateFieldRepository.save(e);
    }

    return templateFieldRepository
        .findById(req.id())
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  TemplateFieldEntity e = TemplateFieldMapper.INSTANCE.toEntity(req);
                  e.setId(req.id());
                  e.setTemplateId(templateId);
                  e.setDeleted(Boolean.FALSE);
                  return templateFieldRepository.save(e);
                }))
        .flatMap(
            existing -> {
              TemplateFieldMapper.INSTANCE.updateEntity(req, existing);
              existing.setDeleted(Boolean.FALSE);
              return templateFieldRepository.save(existing);
            });
  }

  private Flux<TemplateFieldResponse> loadResponse(UUID templateId) {
    return templateFieldRepository
        .findAllByTemplateIdAndDeletedFalse(templateId)
        .map(TemplateFieldMapper.INSTANCE::toResponse);
  }

  public Mono<TemplateFieldResponse> delete(final UUID id) {
    return templateFieldRepository
        .findById(id)
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              return templateFieldRepository
                  .save(entity)
                  .map(TemplateFieldMapper.INSTANCE::toResponse);
            });
  }
}
