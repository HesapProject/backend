package uz.hesap.service.document.service.c2c;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.ClaimEntity;
import uz.hesap.service.document.domain.enums.NoticeStatus;
import uz.hesap.service.document.domain.enums.ClaimStatus;
import uz.hesap.service.document.mapper.ClaimMapper;
import uz.hesap.service.document.model.request.ClaimCreateRequest;
import uz.hesap.service.document.model.response.ClaimResponse;
import uz.hesap.service.document.repository.NoticeRepository;
import uz.hesap.service.document.repository.ClaimRepository;
import uz.hesap.service.document.repository.ClaimTemplateRepository;
import uz.hesap.service.document.service.document.DocumentGenerator;
import uz.hesap.service.document.service.document.DocumentQueryHelper;

// Da'vo arizasi (report/claim) servisi
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimsService {

  private final ClaimRepository claimRepository;
  private final NoticeRepository noticeRepository;
  private final ClaimTemplateRepository claimTemplateRepository;
  private final ClaimMapper claimMapper;
  private final DocumentQueryHelper documentQueryHelper;
  private final DocumentGenerator documentGenerator;
  private final uz.hesap.service.document.service.template.TemplateNotificationDispatcher
      notificationDispatcher;

  // ================ CREATE ================

  // Da'vo arizasi yaratish — kamida 2 ta CREATED talabnoma kerak
  @Transactional
  public Mono<ClaimResponse> create(ClaimCreateRequest request, UUID userId) {
    UUID documentId = request.documentId();
    log.info("Report yaratish, document: {}, user: {}", documentId, userId);

    return documentQueryHelper
        .findAndValidateParty(documentId, userId)
        .flatMap(
            document ->
                checkNoExistingReport(documentId)
                    .then(checkMinimumNotices(documentId))
                    .then(buildAndSaveReport(document, userId)));
  }

  // ================ GET ALL ================

  // Hujjat bo'yicha barcha reportlar (pagination)
  public Mono<Page<ClaimResponse>> getAllByDocument(
      UUID documentId, UUID userId, Pageable pageable) {
    return claimRepository
        .findByContractIdAndDeletedFalse(documentId, pageable)
        .map(claimMapper::toResponse)
        .collectList()
        .zipWith(claimRepository.countByContractIdAndDeletedFalse(documentId))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  // Menga kelgan barcha da'vo arizalari (boshqa tomon yaratgan), pagination
  // Da'vo arizalari — fromIn (yuboruvchi PINFL) / toIn (qabul qiluvchi PINFL) bo'yicha filtr.
  public Mono<Page<ClaimResponse>> getFiltered(String fromIn, String toIn, Pageable pageable) {
    return claimRepository
        .findFiltered(fromIn, toIn, pageable.getPageSize(), pageable.getOffset())
        .map(claimMapper::toResponse)
        .collectList()
        .zipWith(claimRepository.countFiltered(fromIn, toIn))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  // ================ PDF ================

  // Da'vo arizasi PDF — klonlangan templateJson + ota-hujjat (document) datasidan render.
  public Mono<byte[]> generatePdf(UUID reportId) {
    return claimRepository
        .findById(reportId)
        .switchIfEmpty(Mono.error(new NotFoundException("Da'vo arizasi topilmadi")))
        .flatMap(
            report ->
                documentGenerator.generateChild(
                    report.getContractId(), report.getTemplateJson(), null, report.getNumber()));
  }

  // ================ PRIVATE HELPERS — VALIDATSIYA ================

  // allaqachon CREATED report bor-yo'qligi
  private Mono<Void> checkNoExistingReport(UUID documentId) {
    return claimRepository
        .findFirstByContractIdAndStatusAndDeletedFalseOrderByCreatedDateDesc(
            documentId, ClaimStatus.CREATED)
        .flatMap(
            existing ->
                Mono.<Void>error(
                    new InvalidOperationException("Dalolatnoma allaqachon yaratilgan")))
        .then();
  }

  // kamida 2 ta CREATED talabnoma borligini tekshirish
  private Mono<Void> checkMinimumNotices(UUID documentId) {
    return noticeRepository
        .countByContractIdAndStatusAndDeletedFalse(documentId, NoticeStatus.CREATED)
        .flatMap(
            noticeCount -> {
              if (noticeCount < 2) {
                return Mono.error(
                    new InvalidOperationException(
                        "Kamida 2 marta talabnoma jo'natilgan bo'lishi kerak"));
              }
              return Mono.empty();
            });
  }

  // ================ PRIVATE HELPERS — BUILD ================

  // report entity yaratish va saqlash
  private Mono<ClaimResponse> buildAndSaveReport(DocumentEntity document, UUID userId) {
    return claimRepository
        .countByContractIdAndDeletedFalse(document.getId())
        .flatMap(
            reportCount -> {
              int reportNumber = reportCount.intValue() + 1;

              return Mono.justOrEmpty(document.getTemplateId())
                  .flatMap(
                      tId ->
                          claimTemplateRepository.findFirstByContractTemplateIdAndDeletedFalse(
                              tId))
                  .switchIfEmpty(Mono.error(new NotFoundException("Report shabloni topilmadi")))
                  .flatMap(
                      template -> {
                        ClaimEntity entity = new ClaimEntity();
                        entity.setContractId(document.getId());
                        entity.setFromIn(document.getSellerIn());
                        entity.setToIn(document.getBuyerIn());
                        entity.setNameUz(template.getNameUz() + reportNumber);
                        entity.setNameRu(template.getNameRu() + reportNumber);
                        entity.setNameEn(template.getNameEn() + reportNumber);
                        entity.setNumber(reportNumber);
                        entity.setStatus(ClaimStatus.CREATED);
                        entity.setTemplateJson(template.getTemplateData());
                        return claimRepository
                            .save(entity)
                            .delayUntil(saved -> sendReportNotification(saved, userId))
                            .map(claimMapper::toResponse);
                      });
            });
  }

  // ================ PRIVATE HELPERS — NOTIFICATION ================

  // debtorga notification yuboriladi (recipient UUID PINFL'dan tiklanadi — taraflar PINFL'da)
  private Mono<Void> sendReportNotification(ClaimEntity report, UUID creatorId) {
    return documentQueryHelper
        .findOrThrow(report.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveOppositePartyUserId(doc, creatorId)
                    .flatMap(
                        recipientId ->
                            notificationDispatcher.dispatch(
                                doc.getTemplateId(),
                                uz.hesap.service.common.util.message.NotificationEvent.CLAIM,
                                recipientId,
                                doc.getNumber(),
                                null,
                                doc.getId())))
        .then();
  }
}
