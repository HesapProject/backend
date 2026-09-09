package uz.hesap.service.document.service.template;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.domain.template.ClaimTemplateEntity;
import uz.hesap.service.document.domain.template.NoticeTemplateEntity;
import uz.hesap.service.document.model.mapper.TemplateMapper;
import uz.hesap.service.document.model.request.TemplateRequest;
import uz.hesap.service.document.model.response.TemplateResponse;
import uz.hesap.service.document.repository.ClaimTemplateRepository;
import uz.hesap.service.document.repository.NoticeTemplateRepository;
import uz.hesap.service.document.repository.TemplateRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class TemplatesService {
  private final TemplateRepository templateRepository;
  private final NoticeTemplateRepository noticeTemplateRepository;
  private final ClaimTemplateRepository claimTemplateRepository;

  // status — ixtiyoriy filtr (masalan PUBLISHED, mijoz-ilovalar shartnoma yaratishda
  // faqat nashr qilingan shablonlarni ko'rishi uchun). Berilmasa hammasi qaytadi
  // (Control admin panelida shablonlarni boshqarish uchun — CREATED/BLOCKED ham kerak).
  public Flux<TemplateResponse> getAll(final TemplateType type, final TemplateStatus status) {
    // Notice/Claim shablonlari alohida jadvallarga ko'chgan — NOTICE/REPORT
    // so'rov yangi jadvallardan TemplateResponse'ga moslab qaytariladi (backward-compat).
    if (type == TemplateType.NOTICE) {
      return noticeTemplateRepository
          .findAllByDeletedFalse()
          .map(this::toResponseFromNotice)
          .filter(t -> status == null || t.status() == status);
    }
    if (type == TemplateType.REPORT) {
      return claimTemplateRepository
          .findAllByDeletedFalse()
          .map(this::toResponseFromClaim)
          .filter(t -> status == null || t.status() == status);
    }
    Flux<TemplateResponse> all =
        type != null
            ? templateRepository
                .findAllByTemplateTypeAndDeletedFalseOrderByPriorityDescCreatedDateAsc(type)
                .map(TemplateMapper.INSTANCE::toResponse)
            : templateRepository
                .findAllByDeletedFalseOrderByPriorityDescCreatedDateAsc()
                .map(TemplateMapper.INSTANCE::toResponse);
    return status == null ? all : all.filter(t -> t.status() == status);
  }

  public Mono<TemplateResponse> getById(final UUID id) {
    // Avval contract_template, topilmasa notice_template, keyin claim_template.
    return templateRepository
        .findById(id)
        .map(TemplateMapper.INSTANCE::toResponse)
        .switchIfEmpty(noticeTemplateRepository.findById(id).map(this::toResponseFromNotice))
        .switchIfEmpty(claimTemplateRepository.findById(id).map(this::toResponseFromClaim));
  }

  // NoticeTemplateEntity → TemplateResponse: minimal sxema, qolgan field'lar null.
  private TemplateResponse toResponseFromNotice(NoticeTemplateEntity e) {
    return new TemplateResponse(
        e.getId(), e.getContractTemplateId(), e.getCompanyId(),
        e.getNameUz(), e.getNameRu(), e.getNameEn(),
        null, null, null, null, null, null,
        null, null, null,
        e.getTemplateData(), null, null,
        e.getStatus() != null ? e.getStatus() : TemplateStatus.CREATED,
        TemplateType.NOTICE,
        e.getIndividualVerificationType(), e.getLegalVerificationType(),
        null, null, null, null, null, null, null, null, null, null,
        null,
        e.getDeleted(), e.getCreatedDate(), e.getLastModifiedDate());
  }

  // ClaimTemplateEntity → TemplateResponse: bir xil minimal proxy.
  private TemplateResponse toResponseFromClaim(ClaimTemplateEntity e) {
    return new TemplateResponse(
        e.getId(), e.getContractTemplateId(), e.getCompanyId(),
        e.getNameUz(), e.getNameRu(), e.getNameEn(),
        null, null, null, null, null, null,
        null, null, null,
        e.getTemplateData(), null, null,
        e.getStatus() != null ? e.getStatus() : TemplateStatus.CREATED,
        TemplateType.REPORT,
        e.getIndividualVerificationType(), e.getLegalVerificationType(),
        null, null, null, null, null, null, null, null, null, null,
        null,
        e.getDeleted(), e.getCreatedDate(), e.getLastModifiedDate());
  }

  public Mono<TemplateResponse> create(final TemplateRequest request) {
    return templateRepository
        .save(TemplateMapper.INSTANCE.toEntity(request))
        .map(TemplateMapper.INSTANCE::toResponse);
  }

  public Mono<TemplateResponse> edit(final UUID id, final TemplateRequest request) {
    return templateRepository
        .findById(id)
        .flatMap(
            entity -> {
              TemplateMapper.INSTANCE.updateEntity(request, entity);
              return templateRepository.save(entity).map(TemplateMapper.INSTANCE::toResponse);
            });
  }

  public Mono<TemplateResponse> delete(final UUID id) {
    return templateRepository
        .findById(id)
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              return templateRepository.save(entity).map(TemplateMapper.INSTANCE::toResponse);
            });
  }

  public Flux<TemplateBasicResponse> getTemplateNames(List<UUID> ids) {
    return templateRepository
        .findAllById(ids)
        .map(
            entity ->
                new TemplateBasicResponse(
                    entity.getId(),
                    new TextModel(entity.getNameUz(), entity.getNameRu(), entity.getNameEn()),
                    new TextModel(
                        entity.getSellerNameUz(),
                        entity.getSellerNameRu(),
                        entity.getSellerNameEn()),
                    new TextModel(
                        entity.getBuyerNameUz(),
                        entity.getBuyerNameRu(),
                        entity.getBuyerNameEn()),
                    entity.getExchangeMode() != null
                        ? entity.getExchangeMode().name()
                        : "GOODS"));
  }
}
