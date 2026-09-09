package uz.hesap.service.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.LegalDocumentEntity;
import uz.hesap.service.main.domain.LegalDocumentType;
import uz.hesap.service.main.model.DocsResponse;
import uz.hesap.service.main.model.mapper.LegalDocumentMapper;
import uz.hesap.service.main.repository.LegalDocumentRepository;

// Hujjatlar (about/terms/privacy) — bitta LegalDocument, type bilan ajratiladi.
@Log4j2
@Service
@RequiredArgsConstructor
public class LegalDocumentService {

  private final LegalDocumentRepository legalDocumentRepository;

  // Type bo'yicha hujjatni o'qish (hammaga ochiq).
  public Mono<DocsResponse> getDoc(LegalDocumentType type) {
    return legalDocumentRepository
        .findFirstByTypeOrderByCreatedDateDesc(type)
        .map(LegalDocumentService::toDocs);
  }

  // Type bo'yicha hujjatni yaratish/yangilash (faqat admin).
  public Mono<DocsResponse> saveDoc(LegalDocumentType type, TextModel content) {
    log.debug("Save legal document type={} content={}", type, content);
    return save(type, content).map(LegalDocumentService::toDocs);
  }

  private Mono<LegalDocumentEntity> save(LegalDocumentType type, TextModel request) {
    return legalDocumentRepository
        .findFirstByTypeOrderByCreatedDateDesc(type)
        .flatMap(
            existingDoc -> {
              LegalDocumentEntity updatedEntity =
                  LegalDocumentMapper.INSTANCE.update(existingDoc, request);
              updatedEntity.setType(type);
              return legalDocumentRepository.save(updatedEntity);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  LegalDocumentEntity newEntity = LegalDocumentMapper.INSTANCE.toEntity(request);
                  newEntity.setType(type);
                  return legalDocumentRepository.save(newEntity);
                }));
  }

  private static DocsResponse toDocs(LegalDocumentEntity e) {
    return new DocsResponse(
        e.getId(),
        e.getType(),
        new TextModel(e.getTextUz(), e.getTextRu(), e.getTextEn()),
        e.getCreatedDate());
  }
}
