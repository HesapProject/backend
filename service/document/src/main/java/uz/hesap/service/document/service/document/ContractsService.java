package uz.hesap.service.document.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.common.util.enums.WebhookEventType;
import uz.hesap.service.document.domain.document.ContractProductEntity;
import uz.hesap.service.document.domain.CurrencyEntity;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.document.DocumentValueEntity;
import uz.hesap.service.document.domain.document.WitnessRequestEntity;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.DocumentPurpose;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.enums.RoumingType;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.model.mapper.DocumentMapper;
import uz.hesap.service.document.model.request.ContractProductRequest;
import uz.hesap.service.document.model.request.DocumentRequest;
import uz.hesap.service.document.model.request.DocumentValueRequest;
import uz.hesap.service.document.model.response.*;
import uz.hesap.service.document.repository.*;
import uz.hesap.service.document.webclient.UserServiceClient;

@Log4j2
@Service
@RequiredArgsConstructor
public class ContractsService {
  private final DocumentRepository documentRepository;
  private final DocumentValueRepository documentValueRepository;
  private final UserServiceClient userServiceClient;
  private final CustomDocumentRepository customDocumentRepository;
  private final TemplateRepository templateRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final WitnessRequestRepository witnessRequestRepository;
  private final ContractProductRepository contractProductRepository;
  private final CurrencyRepository currencyRepository;
  private final WebhookEventPublisher webhookEventPublisher;
  private final ObjectMapper objectMapper;

  // ================ GET (ENRICHED) ================

  // Admin uchun barcha hujjatlar — userId/status/templateId/search optional filter.
  // search — hujjat raqami (number) bo'yicha qidiruv (global search uchun).
  public Mono<Page<DocumentEnrichedResponse>> getAllAdmin(
      final String in,
      final DocumentStatus status,
      final UUID templateId,
      final String search,
      final Pageable pageable) {
    return customDocumentRepository
        .findFiltered(
            in, status != null ? List.of(status) : null, templateId, search, null, null, pageable)
        .collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(new PageImpl<>(List.of(), pageable, 0));
              }
              Mono<MetadataBundle> metaMono = fetchMetadata(entities);
              Mono<Long> countMono =
                  customDocumentRepository.countFiltered(
                      in, status != null ? List.of(status) : null, templateId, search, null, null);
              return Mono.zip(metaMono, countMono)
                  .map(
                      tuple -> {
                        MetadataBundle meta = tuple.getT1();
                        List<DocumentEnrichedResponse> enriched =
                            entities.stream()
                                .map(e -> enrichSingleDocument(e, List.of(), meta))
                                .toList();
                        return new PageImpl<>(enriched, pageable, tuple.getT2());
                      });
            });
  }

  // barcha hujjatlar — dinamik filter + enrichment
  // `in` (taraf identifikatori, PINFL/STIR) bo'yicha — buyer_in YOKI seller_in mos
  // kelgan shartnomalar. User o'chsa ham bog'lanish saqlanadi (connection uzilmaydi).
  public Mono<Page<DocumentEnrichedResponse>> getAll(
      final String in,
      final Pageable pageable,
      final List<DocumentStatus> statuses,
      final UUID templateId,
      final Boolean withValues,
      final String search,
      final Instant from,
      final Instant to) {
    return customDocumentRepository
        .findFiltered(in, statuses, templateId, search, from, to, pageable)
        .collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(new PageImpl<>(List.of(), pageable, 0));
              }
              // metadata (user + template) — batch fetch
              Mono<MetadataBundle> metaMono = fetchMetadata(entities);
              // agar withValues=true bo'lsa — har bir hujjat uchun values olish
              Mono<Map<UUID, List<DocumentValueResponse>>> valuesMono =
                  withValues ? fetchValuesForDocuments(entities) : Mono.just(Map.of());
              Mono<Long> countMono =
                  customDocumentRepository.countFiltered(in, statuses, templateId, search, from, to);

              return Mono.zip(metaMono, valuesMono, countMono)
                  .map(
                      tuple -> {
                        MetadataBundle meta = tuple.getT1();
                        Map<UUID, List<DocumentValueResponse>> valuesMap = tuple.getT2();
                        List<DocumentEnrichedResponse> enriched =
                            entities.stream()
                                .map(
                                    e ->
                                        enrichSingleDocument(
                                            e, valuesMap.getOrDefault(e.getId(), List.of()), meta))
                                .toList();
                        return new PageImpl<>(enriched, pageable, tuple.getT3());
                      });
            });
  }

  // Berilgan ID'lar bo'yicha shartnomalarni enrichment bilan qaytaradi (guvohlik Home
  // kartasi uchun — witness scope contracts endpoint'ida yo'q, shu sabab id bo'yicha).
  // Tartib: eng yangi (createdDate desc). Deleted o'tkazib yuboriladi.
  public Mono<List<DocumentEnrichedResponse>> enrichByIds(final List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Mono.just(List.of());
    }
    return documentRepository
        .findAllById(ids)
        .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
        .collectSortedList(
            (a, b) -> {
              Instant ca = a.getCreatedDate();
              Instant cb = b.getCreatedDate();
              if (ca == null || cb == null) return 0;
              return cb.compareTo(ca);
            })
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(List.of());
              }
              return fetchMetadata(entities)
                  .map(
                      meta ->
                          entities.stream()
                              .map(e -> enrichSingleDocument(e, List.of(), meta))
                              .toList());
            });
  }

  // O'zaro shartnomalar — ikki taraf (inA, inB) orasidagi shartnomalar, enrichment bilan.
  public Mono<Page<DocumentEnrichedResponse>> getBetween(
      final String inA, final String inB, final Pageable pageable) {
    return customDocumentRepository
        .findBetweenParties(inA, inB, pageable)
        .collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.just(new PageImpl<>(List.of(), pageable, 0));
              }
              Mono<MetadataBundle> metaMono = fetchMetadata(entities);
              Mono<Long> countMono = customDocumentRepository.countBetweenParties(inA, inB);
              return Mono.zip(metaMono, countMono)
                  .map(
                      tuple -> {
                        MetadataBundle meta = tuple.getT1();
                        List<DocumentEnrichedResponse> enriched =
                            entities.stream()
                                .map(e -> enrichSingleDocument(e, List.of(), meta))
                                .toList();
                        return new PageImpl<>(enriched, pageable, tuple.getT2());
                      });
            });
  }

  // Taraf (in) shartnomalari soni status bo'yicha — filter chiplar uchun.
  public Mono<Map<DocumentStatus, Long>> getStatusCounts(final String in) {
    return customDocumentRepository
        .countByStatus(in)
        .collectMap(
            CustomDocumentRepository.DocumentStatusCount::status,
            CustomDocumentRepository.DocumentStatusCount::count);
  }

  // bitta hujjat — values + company/user/template bilan boyitilgan
  public Mono<DocumentEnrichedResponse> getById(final UUID id) {
    return documentRepository
        .findById(id)
        .flatMap(
            document ->
                // values va metadata ni parallel olish
                Mono.zip(
                        fetchDocumentValues(document.getId()),
                        fetchMetadata(List.of(document)),
                        contractProductRepository
                            .findAllByDocumentIdAndDeletedFalse(document.getId())
                            .map(this::toProductResponse)
                            .collectList())
                    .map(
                        tuple -> {
                          List<DocumentValueResponse> values = tuple.getT1();
                          MetadataBundle meta = tuple.getT2();
                          List<ContractProductResponse> products = tuple.getT3();
                          return enrichSingleDocument(document, values, meta, products);
                        }));
  }

  // ================ CREATE ================

  @Transactional
  public Mono<UUID> create(
      final DocumentRequest request, final UUID createdByUser, final String creatorIn) {
    // Raqamni backend generatsiya qiladi (YYMMDD-NNNN, kunlik ketma-ket, unikal).
    // request.number() e'tiborga olinmaydi.
    return requireProductsIfNeeded(request)
        .then(generateDocumentNumber())
        .flatMap(
            number -> {
              DocumentEntity entity = DocumentMapper.INSTANCE.toEntity(request);
              entity.setNumber(number);
              // Taraflar barqaror identifikatori (buyer_in/seller_in) to'g'ridan-to'g'ri
              // request'dan keladi. Yuboruvchi tomon bo'sh bo'lsa (frontend faqat
              // qarama-qarshi tomon in'ini yuboradi) — principal identifier'i bilan to'ldiramiz.
              if (entity.getBuyerIn() == null || entity.getBuyerIn().isBlank()) {
                entity.setBuyerIn(creatorIn);
              }
              if (entity.getSellerIn() == null || entity.getSellerIn().isBlank()) {
                entity.setSellerIn(creatorIn);
              }
              // Kim yaratganini saqlaymiz — ko'rinish qoidasi (yaratuvchi imzolamaguncha
              // qarshi tomonga ko'rinmaydi) va imzolashda paket hisobi shu maydonga tayanadi.
              entity.setCreatorIn(creatorIn);
              // Boshlang'ich holat: status CREATED (pending), har ikki taraf imzosi kutiladi.
              // Kim imzolagani buyerStatus/sellerStatus maydonlaridan aniqlanadi (statusdan emas).
              entity.setStatus(DocumentStatus.CREATED);
              entity.setBuyerStatus(DocumentPartyStatus.PENDING);
              entity.setSellerStatus(DocumentPartyStatus.PENDING);
              // Public ko'rish uchun 4 xonali kod (contract.hesap.uz PIN + PDF QR yonida).
              entity.setAccessCode(
                  String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000)));
              // Hujjat maqsadi — request'da kelmasa, shablonning purpose'idan (bo'lmasa CONTRACT).
              // currency_id ni currencies jadvalidan hal qilamiz (currencyId yoki kod bo'yicha).
              return resolvePurpose(entity, request.templateId())
                  .flatMap(e -> applyCurrency(e, request))
                  .flatMap(documentRepository::save);
            })
        .flatMap(
            document ->
                saveDocumentValues(document, request)
                    .then(savePayments(document, request))
                    .then(saveDocumentWitnesses(document.getId(), request))
                    .then(saveContractProducts(document.getId(), request))
                    // Paket endi YARATISHDA emas, yaratuvchi O'Z IMZOSINI qo'yganda
                    // hisobdan yechiladi (C2CDocumentActionService.applyPartyAccept).
                    // OpenAPI webhook — hamkor tizimi shartnoma yaratilganini bilsin.
                    .doOnSuccess(
                        ignored ->
                            webhookEventPublisher.publish(
                                WebhookEventType.CONTRACT_CREATED, document, creatorIn))
                    // Yaratilgan hujjat id'sini qaytaramiz (mobil yangi shartnomaga o'tishi uchun).
                    .thenReturn(document.getId()));
  }

  // currency'ni currencies jadvalidan hal qiladi: currencyId berilsa id bo'yicha, aks holda
  // currency kodi bo'yicha. Topilsa entity.currencyId + currency (enum) ikkalasini qo'yadi.
  private Mono<DocumentEntity> applyCurrency(DocumentEntity entity, DocumentRequest request) {
    Mono<CurrencyEntity> lookup;
    if (request.currencyId() != null) {
      lookup = currencyRepository.findByIdAndDeletedFalse(request.currencyId());
    } else if (request.currency() != null) {
      lookup = currencyRepository.findByCodeAndDeletedFalse(request.currency().name());
    } else {
      return Mono.just(entity);
    }
    return lookup
        .map(
            cur -> {
              entity.setCurrencyId(cur.getId());
              try {
                entity.setCurrency(Currency.valueOf(cur.getCode()));
              } catch (IllegalArgumentException ignored) {
                // currencies.code Currency enum'da bo'lmasa — currency o'zgarmaydi.
              }
              return entity;
            })
        .defaultIfEmpty(entity);
  }

  // Guvohlik so'rovlarini (witness_requests) PENDING holatda saqlaydi. Bo'sh bo'lsa o'tkazib yuboriladi.
  private Mono<List<WitnessRequestEntity>> saveDocumentWitnesses(
      UUID documentId, DocumentRequest request) {
    if (request.witnessIds() == null || request.witnessIds().isEmpty()) {
      return Mono.just(List.of());
    }
    var witnessRequests =
        request.witnessIds().stream()
            .map(
                witnessId -> {
                  WitnessRequestEntity entity = new WitnessRequestEntity();
                  entity.setContractId(documentId);
                  entity.setWitnessId(witnessId);
                  entity.setStatus(DocumentWitnessStatus.PENDING);
                  return entity;
                })
            .toList();
    return witnessRequestRepository.saveAll(witnessRequests).collectList();
  }

  // Hujjat maqsadi: request'da aniq kelgan bo'lsa o'shani ishlatamiz; aks holda
  // shablonning roumingType'idan (WAYBILL→TTN, ACT→AKT, FACTURA→FACTURA,
  // qolganlari/NULL→CONTRACT) — shablon yaratishda tanlangan Rouming turi bilan
  // hujjat maqsadi bir xil semantikaga ega, shu sabab qayta ishlatiladi.
  private Mono<DocumentEntity> resolvePurpose(DocumentEntity entity, UUID templateId) {
    if (entity.getPurpose() != null) {
      return Mono.just(entity);
    }
    if (templateId == null) {
      entity.setPurpose(DocumentPurpose.CONTRACT);
      return Mono.just(entity);
    }
    return templateRepository
        .findById(templateId)
        // roumingType NULL bo'lishi mumkin — Reactor map() null qaytarishga yo'l qo'ymaydi,
        // shuning uchun null'ni toDocumentPurpose ichida (CONTRACT) hal qilamiz.
        .map(template -> toDocumentPurpose(template.getRoumingType()))
        .defaultIfEmpty(DocumentPurpose.CONTRACT)
        .map(
            purpose -> {
              entity.setPurpose(purpose);
              return entity;
            });
  }

  private static DocumentPurpose toDocumentPurpose(RoumingType roumingType) {
    if (roumingType == null) {
      return DocumentPurpose.CONTRACT;
    }
    return switch (roumingType) {
      case WAYBILL -> DocumentPurpose.TTN;
      case ACT -> DocumentPurpose.AKT;
      case FACTURA -> DocumentPurpose.FACTURA;
      default -> DocumentPurpose.CONTRACT;
    };
  }

  private Mono<Void> requireProductsIfNeeded(DocumentRequest request) {
    boolean hasProducts = request.products() != null && !request.products().isEmpty();
    if (hasProducts || request.templateId() == null) {
      return Mono.empty();
    }
    return templateRepository
        .findById(request.templateId())
        .flatMap(
            template -> {
              // GOODS (mahsulot) rejimidagi shablonda mahsulotsiz shartnoma bo'lmaydi;
              // qolganlarda productEnabled+productRequired bayrog'iga qaraladi.
              boolean goodsMode =
                  template.getExchangeMode()
                      == uz.hesap.service.document.domain.enums.ExchangeMode.GOODS;
              if (goodsMode
                  || (Boolean.TRUE.equals(template.getProductEnabled())
                      && Boolean.TRUE.equals(template.getProductRequired()))) {
                return Mono.<Void>error(
                    new BadRequestException(
                        "Kamida bitta mahsulot qo'shish shart"));
              }
              return Mono.empty();
            });
  }

  // Shartnoma mahsulotlarini (products) saqlaydi. Bo'sh bo'lsa o'tkazib yuboriladi.
  private Mono<List<ContractProductEntity>> saveContractProducts(
      UUID documentId, DocumentRequest request) {
    if (request.products() == null || request.products().isEmpty()) {
      return Mono.just(List.of());
    }
    var productEntities =
        request.products().stream()
            .map(
                product -> {
                  ContractProductEntity entity = new ContractProductEntity();
                  entity.setDocumentId(documentId);
                  entity.setName(product.name());
                  entity.setUnit(product.unit());
                  entity.setPrice(product.price());
                  entity.setQuantity(product.quantity());
                  entity.setAmount(product.amount());
                  entity.setDeliveryAt(product.deliveryAt());
                  entity.setFieldValues(toJson(product.values()));
                  entity.setDeleted(Boolean.FALSE);
                  return entity;
                })
            .toList();
    return contractProductRepository.saveAll(productEntities).collectList();
  }

  // Map → JSON string (null/bo'sh bo'lsa null).
  private String toJson(Map<String, Object> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      log.warn("Contract product values JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }

  // YYMMDD-NNNN: kunlik atomik hisoblagichdan ketma-ket, unikal raqam.
  private Mono<String> generateDocumentNumber() {
    java.time.LocalDate today = java.time.LocalDate.now();
    return customDocumentRepository
        .nextDailyNumber(today)
        .map(
            no ->
                today.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
                    + "-"
                    + String.format("%04d", no));
  }

  // To'lov jadvali qatorlarini (payments) hujjat bilan saqlaydi. Bo'sh bo'lsa
  // o'tkazib yuboriladi. buyer/seller/currency hujjatdan olinadi.
  private Mono<Void> savePayments(DocumentEntity document, DocumentRequest request) {
    if (request.payments() == null || request.payments().isEmpty()) {
      return Mono.empty();
    }
    List<PaymentEntity> entities =
        request.payments().stream()
            .filter(p -> p != null && p.amount() != null && p.paymentDate() != null)
            .map(
                p -> {
                  PaymentEntity e = new PaymentEntity();
                  e.setContractId(document.getId());
                  e.setBuyerIn(document.getBuyerIn());
                  e.setSellerIn(document.getSellerIn());
                  e.setTotalAmount(p.amount());
                  e.setCurrency(document.getCurrency());
                  e.setCurrencyId(document.getCurrencyId());
                  e.setContractPaymentDate(p.paymentDate());
                  e.setStatus(PaymentScheduleStatus.PENDING);
                  // Shartnoma holati snapshot (yaratishda CREATED).
                  e.setContractStatus(document.getStatus());
                  return e;
                })
            .toList();
    if (entities.isEmpty()) {
      return Mono.empty();
    }
    return paymentScheduleRepository.saveAll(entities).then();
  }

  // ================ EDIT ================

  @Transactional
  public Mono<Void> edit(final UUID id, final DocumentRequest request) {
    return documentRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found: " + id)))
        .flatMap(
            entity -> {
              DocumentMapper.INSTANCE.updateEntity(request, entity);
              return documentRepository.save(entity);
            })
        .flatMap(document -> editDocumentValues(document, request));
  }

  // ================ DELETE ================

  public Mono<Void> delete(final UUID id) {
    return documentRepository
        .findById(id)
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              return documentRepository.save(entity).then();
            });
  }

  public Mono<Void> deleteValue(final UUID id) {
    return documentValueRepository
        .findById(id)
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              return documentValueRepository.save(entity).then();
            });
  }

  // ================ PRIVATE: ENRICHMENT ================

  // creator PINFL/STIR'lari (yaratuvchi ham taraflardek PINFL bilan)
  private Set<String> extractCreatorIns(List<DocumentEntity> entities) {
    return entities.stream()
        .map(DocumentEntity::getCreatorIn)
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.toSet());
  }

  // taraf identifikatorlari (buyer_in/seller_in, PINFL/STIR)
  private Set<String> extractPartyIns(List<DocumentEntity> entities) {
    return entities.stream()
        .flatMap(e -> Stream.of(e.getBuyerIn(), e.getSellerIn()))
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.toSet());
  }

  private Set<UUID> extractTemplateIds(List<DocumentEntity> entities) {
    return entities.stream()
        .map(DocumentEntity::getTemplateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  // tashqi servislar + DB dan batch fetch — N+1 oldini oladi
  private Mono<MetadataBundle> fetchMetadata(List<DocumentEntity> entities) {
    // Taraflar (buyer_in/seller_in) + yaratuvchi (creator_in) — barchasi PINFL, bitta map'da.
    Set<String> partyIns = new java.util.HashSet<>(extractPartyIns(entities));
    partyIns.addAll(extractCreatorIns(entities));
    Set<UUID> templateIds = extractTemplateIds(entities);

    if (partyIns.isEmpty() && templateIds.isEmpty()) {
      return Mono.just(new MetadataBundle(Map.of(), Map.of()));
    }

    // taraflar + creator — PINFL/STIR bo'yicha (tashqi servis)
    Mono<Map<String, UserBasicResponse>> partyUsersMono =
        partyIns.isEmpty() ? Mono.just(Map.of()) : userServiceClient.getUsersBasicByIns(partyIns);

    // template — lokal DB
    Mono<Map<UUID, TemplateBasicResponse>> templatesMono =
        templateIds.isEmpty()
            ? Mono.just(Map.of())
            : templateRepository
                .findAllById(templateIds)
                .map(DocumentMapper.INSTANCE::toTemplateBasic)
                .collectMap(TemplateBasicResponse::id);

    return Mono.zip(partyUsersMono, templatesMono)
        .map(tuple -> new MetadataBundle(tuple.getT1(), tuple.getT2()));
  }

  // bitta hujjatni boyitish — user/template map dan olish
  // ESLATMA: Map.of() immutable map get(null) chaqirilganda NPE tashlaydi —
  // shuning uchun getOrNull() helper ishlatamiz.
  private DocumentEnrichedResponse enrichSingleDocument(
      DocumentEntity entity, List<DocumentValueResponse> values, MetadataBundle meta) {
    return enrichSingleDocument(entity, values, meta, List.of());
  }

  private DocumentEnrichedResponse enrichSingleDocument(
      DocumentEntity entity,
      List<DocumentValueResponse> values,
      MetadataBundle meta,
      List<ContractProductResponse> products) {
    return DocumentMapper.INSTANCE.toEnrichedResponse(
        entity,
        getOrNull(meta.partyUsers(), entity.getBuyerIn()),
        getOrNull(meta.partyUsers(), entity.getSellerIn()),
        getOrNull(meta.partyUsers(), entity.getCreatorIn()),
        getOrNull(meta.templates(), entity.getTemplateId()),
        values,
        products);
  }

  private ContractProductResponse toProductResponse(ContractProductEntity e) {
    return new ContractProductResponse(
        e.getId(), e.getName(), e.getUnit(), e.getPrice(), e.getQuantity(), e.getAmount(),
        e.getDeliveryAt(), null);
  }

  // null key bo'lsa Map.of() immutable map NPE tashlaydi — shuni oldini olamiz
  private static <K, V> V getOrNull(Map<K, V> map, K key) {
    if (map == null || key == null) return null;
    return map.get(key);
  }

  // bitta hujjat uchun values olish
  private Mono<List<DocumentValueResponse>> fetchDocumentValues(UUID documentId) {
    return documentValueRepository
        .findAllByDocumentIdAndDeletedFalse(documentId)
        .map(DocumentMapper.INSTANCE::toValueResponse)
        .collectList();
  }

  // ko'p hujjatlar uchun values ni batch olish — documentId → List<value> map
  private Mono<Map<UUID, List<DocumentValueResponse>>> fetchValuesForDocuments(
      List<DocumentEntity> entities) {
    Set<UUID> docIds = entities.stream().map(DocumentEntity::getId).collect(Collectors.toSet());
    return documentValueRepository
        .findAllByDocumentIdInAndDeletedFalse(docIds)
        .map(DocumentMapper.INSTANCE::toValueResponse)
        .collectList()
        .map(
            list ->
                list.stream().collect(Collectors.groupingBy(DocumentValueResponse::documentId)));
  }

  // ================ PRIVATE: CREATE HELPERS ================

  private Mono<Void> saveDocumentValues(DocumentEntity document, DocumentRequest request) {
    if (request.values() == null || request.values().isEmpty()) return Mono.empty();
    var list =
        request.values().stream()
            .map(
                value -> {
                  DocumentValueEntity entity = new DocumentValueEntity();
                  entity.setDocumentId(document.getId());
                  entity.setPosition(value.position());
                  entity.setValue(value.value());
                  entity.setKeyName(value.keyName());
                  entity.setTemplateId(value.templateId());
                  entity.setTemplateFieldId(value.templateFieldId());
                  return entity;
                })
            .toList();
    return documentValueRepository.saveAll(list).then();
  }

  // ================ PRIVATE: EDIT HELPERS ================

  private Mono<Void> editDocumentValues(DocumentEntity document, DocumentRequest request) {
    List<DocumentValueRequest> reqValues = request.values() == null ? List.of() : request.values();
    List<DocumentValueRequest> cleaned =
        reqValues.stream().filter(v -> v.templateFieldId() != null || v.id() != null).toList();
    Map<UUID, DocumentValueRequest> byId =
        cleaned.stream()
            .filter(v -> v.id() != null)
            .collect(Collectors.toMap(DocumentValueRequest::id, Function.identity(), (a, b) -> b));
    Map<UUID, DocumentValueRequest> byFieldNoId =
        cleaned.stream()
            .filter(v -> v.id() == null && v.templateFieldId() != null)
            .collect(
                Collectors.toMap(
                    DocumentValueRequest::templateFieldId, Function.identity(), (a, b) -> b));

    return documentValueRepository
        .findAllByDocumentIdAndDeletedFalse(document.getId())
        .collectList()
        .flatMapMany(
            existingList -> {
              Map<UUID, DocumentValueEntity> existingById =
                  existingList.stream()
                      .filter(e -> e.getId() != null)
                      .collect(Collectors.toMap(DocumentValueEntity::getId, Function.identity()));
              Map<UUID, DocumentValueEntity> existingByField =
                  existingList.stream()
                      .filter(e -> e.getTemplateFieldId() != null)
                      .collect(
                          Collectors.toMap(
                              DocumentValueEntity::getTemplateFieldId,
                              Function.identity(),
                              (a, b) -> a));

              Set<UUID> touchedExistingIds = new HashSet<>(16, 12);
              List<DocumentValueEntity> toSave = new ArrayList<>(16);

              for (DocumentValueRequest req : byId.values()) {
                DocumentValueEntity entity = existingById.get(req.id());
                if (entity == null) {
                  return Flux.error(
                      new IllegalArgumentException(
                          "DocumentValue id not found for this document: " + req.id()));
                }
                if (!document.getId().equals(entity.getDocumentId())) {
                  return Flux.error(
                      new IllegalArgumentException(
                          "DocumentValue id does not belong to document: " + req.id()));
                }
                touchedExistingIds.add(entity.getId());
                entity.setPosition(req.position());
                entity.setValue(req.value());
                entity.setKeyName(req.keyName());
                entity.setTemplateId(document.getTemplateId());
                if (req.templateFieldId() != null) entity.setTemplateFieldId(req.templateFieldId());
                toSave.add(entity);
              }

              for (DocumentValueRequest req : byFieldNoId.values()) {
                UUID fieldId = req.templateFieldId();
                DocumentValueEntity entity = existingByField.get(fieldId);
                if (entity != null
                    && entity.getId() != null
                    && touchedExistingIds.contains(entity.getId())) {
                  continue;
                }
                if (entity == null) {
                  entity = new DocumentValueEntity();
                  entity.setDocumentId(document.getId());
                  entity.setTemplateId(document.getTemplateId());
                  entity.setTemplateFieldId(fieldId);
                } else {
                  touchedExistingIds.add(entity.getId());
                }
                entity.setPosition(req.position());
                entity.setValue(req.value());
                entity.setKeyName(req.keyName());
                toSave.add(entity);
              }

              for (DocumentValueEntity existing : existingList) {
                UUID existingId = existing.getId();
                if (existingId != null && touchedExistingIds.contains(existingId)) continue;
                toSave.add(existing);
              }

              return documentValueRepository.saveAll(toSave);
            })
        .then();
  }

}
