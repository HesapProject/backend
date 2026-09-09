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
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.NoticeEntity;
import uz.hesap.service.document.domain.enums.NoticeStatus;
import uz.hesap.service.document.mapper.NoticeMapper;
import uz.hesap.service.document.model.request.NoticeCreateRequest;
import uz.hesap.service.document.model.response.NoticeResponse;
import uz.hesap.service.document.repository.NoticeRepository;
import uz.hesap.service.document.repository.NoticeTemplateRepository;
import uz.hesap.service.document.service.document.DocumentGenerator;
import uz.hesap.service.document.service.document.DocumentQueryHelper;
import uz.hesap.service.jms.JmsPublisher;

// Talabnoma (notice/claim) servisi
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private final NoticeTemplateRepository noticeTemplateRepository;
  private final NoticeMapper noticeMapper;
  private final JmsPublisher jmsPublisher;
  private final DocumentQueryHelper documentQueryHelper;
  private final DocumentGenerator documentGenerator;
  private final uz.hesap.service.document.service.template.TemplateNotificationDispatcher
      notificationDispatcher;

  // ================ CREATE ================

  // Talabnoma yaratish — documentId keladi, to'g'ridan-to'g'ri CREATED holati
  @Transactional
  public Mono<NoticeResponse> create(NoticeCreateRequest request, UUID userId) {
    log.info("Talabnoma yaratish, document: {}, user: {}", request.documentId(), userId);

    return documentQueryHelper
        .findAndValidateParty(request.documentId(), userId)
        .flatMap(document -> buildAndSaveNotice(document, userId));
  }

  // ================ GET ALL ================

  // Hujjat bo'yicha barcha talabnomalari (pagination)
  public Mono<Page<NoticeResponse>> getAllByDocument(UUID documentId, Pageable pageable) {
    return noticeRepository
        .findByContractIdAndDeletedFalse(documentId, pageable)
        .map(noticeMapper::toResponse)
        .collectList()
        .zipWith(noticeRepository.countByContractIdAndDeletedFalse(documentId))
        .map(p -> new PageImpl<>(p.getT1(), pageable, p.getT2()));
  }

  // Talabnomalar — fromIn (yuboruvchi PINFL) / toIn (qabul qiluvchi PINFL) bo'yicha filtr.
  public Mono<Page<NoticeResponse>> getFiltered(String fromIn, String toIn, Pageable pageable) {
    return noticeRepository
        .findFiltered(fromIn, toIn, pageable.getPageSize(), pageable.getOffset())
        .map(noticeMapper::toResponse)
        .collectList()
        .zipWith(noticeRepository.countFiltered(fromIn, toIn))
        .map(p -> new PageImpl<>(p.getT1(), pageable, p.getT2()));
  }

  // ================ PDF ================

  // Talabnoma PDF — klonlangan templateJson + ota-hujjat (document) datasidan render.
  public Mono<byte[]> generatePdf(UUID noticeId) {
    return noticeRepository
        .findById(noticeId)
        .switchIfEmpty(Mono.error(new NotFoundException("Talabnoma topilmadi")))
        .flatMap(
            notice ->
                documentGenerator.generateChild(
                    notice.getContractId(), notice.getTemplateJson(), notice.getNumber(), null));
  }

  // ================ PRIVATE HELPERS ================

  // notice yaratish — template klonlash + notification
  private Mono<NoticeResponse> buildAndSaveNotice(DocumentEntity document, UUID userId) {
    return noticeRepository
        .countByContractIdAndDeletedFalse(document.getId())
        .flatMap(
            count -> {
              int noticeNumber = count.intValue() + 1;

              return Mono.justOrEmpty(document.getTemplateId())
                  .flatMap(
                      tId ->
                          noticeTemplateRepository.findFirstByContractTemplateIdAndDeletedFalse(
                              tId))
                  .switchIfEmpty(Mono.error(new NotFoundException("Talabnoma shabloni topilmadi")))
                  .flatMap(
                      template -> {
                        NoticeEntity entity = new NoticeEntity();
                        entity.setContractId(document.getId());
                        entity.setBuyerIn(document.getBuyerIn());
                        entity.setSellerIn(document.getSellerIn());
                        entity.setNameUz("Talabnoma № " + noticeNumber);
                        entity.setNameRu("Претензионное письмо № " + noticeNumber);
                        entity.setNameEn("Notice Letter No. " + noticeNumber);
                        entity.setNumber(noticeNumber);
                        entity.setStatus(NoticeStatus.CREATED);
                        entity.setTemplateJson(template.getTemplateData());
                        return noticeRepository
                            .save(entity)
                            // Notification xatosi talabnoma yaratilishini BUZMASIN (onErrorResume):
                            // qarama-qarshi taraf ilovada topilmasa yoki push xato bo'lsa ham
                            // talabnoma saqlanib qolsin (aks holda @Transactional rollback → 400).
                            .delayUntil(
                                saved ->
                                    sendNoticeNotification(saved, userId)
                                        .onErrorResume(
                                            e -> {
                                              log.error(
                                                  "Talabnoma notification xatosi (talabnoma"
                                                      + " saqlandi): {}",
                                                  e.getMessage());
                                              return Mono.empty();
                                            }))
                            .map(noticeMapper::toResponse);
                      });
            });
  }

  // debtorga notification yuborish (recipient UUID PINFL'dan tiklanadi — taraflar PINFL'da)
  private Mono<Void> sendNoticeNotification(NoticeEntity notice, UUID creatorId) {
    return documentQueryHelper
        .findOrThrow(notice.getContractId())
        .flatMap(
            doc ->
                documentQueryHelper
                    .resolveOppositePartyUserId(doc, creatorId)
                    .flatMap(
                        recipientId ->
                            notificationDispatcher.dispatch(
                                doc.getTemplateId(),
                                uz.hesap.service.common.util.message.NotificationEvent.NOTICE,
                                recipientId,
                                doc.getNumber(),
                                null,
                                doc.getId())))
        .then();
  }
}
