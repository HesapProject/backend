package uz.hesap.service.document.service.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.document.domain.template.TemplateFieldEntity;
import uz.hesap.service.document.model.mapper.TemplateFieldMapper;
import uz.hesap.service.document.model.request.TemplateFieldRequest;
import uz.hesap.service.document.model.response.CompileCheckResponse;
import uz.hesap.service.document.model.response.TemplateFieldResponse;
import uz.hesap.service.document.model.response.TemplateStructureResponse;
import uz.hesap.service.document.repository.TemplateFieldRepository;
import uz.hesap.service.document.repository.TemplateRepository;
import uz.hesap.service.document.service.document.structure.JrxmlBuilder;
import uz.hesap.service.document.service.document.structure.StructureFieldSync;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure;

/**
 * Blok-konstruktor servisi: struktura JSON'ni saqlaydi → JRXML generatsiya qiladi
 * ({@code template_data}) → template_field'larni sinxronlaydi. Eski raw-JRXML shablonlarga
 * tegmaydi (struktura faqat shu yerda boshqariladi).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TemplateStructureService {

  private final TemplateRepository templateRepository;
  private final TemplateFieldRepository templateFieldRepository;
  private final TemplateFieldService templateFieldService;
  private final Jackson2ObjectMapperBuilder mapperBuilder;

  private ObjectMapper mapper() {
    return mapperBuilder.build();
  }

  /** Shablonning saqlangan strukturasi (parse qilingan JSON obyekt) — yoki bo'sh. */
  public Mono<Object> getStructure(final UUID templateId) {
    return templateRepository
        .findById(templateId)
        .switchIfEmpty(Mono.error(new NotFoundException("Shablon topilmadi")))
        .flatMap(
            e -> {
              String json = e.getTemplateStructure();
              if (json == null || json.isBlank()) {
                return Mono.empty();
              }
              try {
                return Mono.just(mapper().readValue(json, Object.class));
              } catch (Exception ex) {
                return Mono.error(new BadRequestException("Saqlangan struktura buzilgan"));
              }
            });
  }

  /** Strukturani saqlaydi, uchala tilda (uz/ru/en) JRXML generatsiya qiladi, field'larni sinxronlaydi. */
  @Transactional
  public Mono<TemplateStructureResponse> saveStructure(
      final UUID templateId, final String structureJson) {
    // Sarlavha + taraf nomlari template'ning o'zidan olinadi (struktura'da qayta kiritilmaydi).
    return templateRepository
        .findById(templateId)
        .switchIfEmpty(Mono.error(new NotFoundException("Shablon topilmadi")))
        .flatMap(
            entity ->
                Mono.fromCallable(() -> buildAllLangs(parse(structureJson), ctxOf(entity)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(
                        jrxmlByLang -> {
                          entity.setTemplateStructure(structureJson);
                          entity.setTemplateDataUz(jrxmlByLang.get("uz"));
                          entity.setTemplateDataRu(jrxmlByLang.get("ru"));
                          entity.setTemplateDataEn(jrxmlByLang.get("en"));
                          return templateRepository
                              .save(entity)
                              .then(syncFields(templateId, structureJson))
                              .map(
                                  fields ->
                                      new TemplateStructureResponse(
                                          structureJson, jrxmlByLang.get("uz"), fields));
                        }));
  }

  /**
   * Barcha strukturaga ega shablonlarning JRXML'sini (uz/ru/en) qayta quradi. JrxmlBuilder
   * o'zgargach (masalan party manzili) yoki struktura'ga yangi tarjima qo'shilgach mavjud
   * shablonlarni yangilash uchun. Struktura buzuq/bo'sh bo'lsa — o'sha shablon o'tkazib
   * yuboriladi. Qaytadi: yangilangan soni.
   */
  // Diagnostika bilan rebuild: har bir shablon uchun natija (ok / xato matni).
  public Mono<Map<String, String>> rebuildAllJrxmlDetailed() {
    return templateRepository
        .findAll()
        .flatMap(
            entity ->
                Mono.fromCallable(
                        () -> {
                          if (Boolean.TRUE.equals(entity.getDeleted())) {
                            return Map.entry(entity.getId().toString(), "skip: deleted");
                          }
                          if (entity.getTemplateStructure() == null
                              || entity.getTemplateStructure().isBlank()) {
                            return Map.entry(entity.getId().toString(), "skip: structure yo'q");
                          }
                          Map<String, String> jrxmlByLang =
                              buildAllLangs(parse(entity.getTemplateStructure()), ctxOf(entity));
                          entity.setTemplateDataUz(jrxmlByLang.get("uz"));
                          entity.setTemplateDataRu(jrxmlByLang.get("ru"));
                          entity.setTemplateDataEn(jrxmlByLang.get("en"));
                          return Map.entry(entity.getId().toString(), "built");
                        })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(
                        e ->
                            "built".equals(e.getValue())
                                ? templateRepository
                                    .save(entity)
                                    .thenReturn(
                                        Map.entry(entity.getId().toString(), "ok"))
                                : Mono.just(e))
                    .onErrorResume(
                        err ->
                            Mono.just(
                                Map.entry(
                                    entity.getId() == null ? "?" : entity.getId().toString(),
                                    "XATO: " + err))))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  public Mono<Integer> rebuildAllJrxml() {
    return templateRepository
        .findAllByDeletedFalseOrderByPriorityDescCreatedDateAsc()
        .filter(e -> e.getTemplateStructure() != null && !e.getTemplateStructure().isBlank())
        .flatMap(
            entity ->
                Mono.fromCallable(
                        () -> buildAllLangs(parse(entity.getTemplateStructure()), ctxOf(entity)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(
                        jrxmlByLang -> {
                          entity.setTemplateDataUz(jrxmlByLang.get("uz"));
                          entity.setTemplateDataRu(jrxmlByLang.get("ru"));
                          entity.setTemplateDataEn(jrxmlByLang.get("en"));
                          return templateRepository.save(entity);
                        })
                    .onErrorResume(
                        e -> {
                          log.error(
                              "rebuildAllJrxml xato [{}]: {}", entity.getId(), e.toString(), e);
                          return Mono.empty();
                        }))
        .count()
        .map(Long::intValue);
  }

  // Bitta strukturadan uchala tilda JRXML quradi — natijada har doim "uz"/"ru"/"en"
  // kalitlari mavjud (ba'zi til uchun tarjima bo'lmasa ham, JrxmlBuilder uz'ga fallback qiladi).
  private static Map<String, String> buildAllLangs(
      TemplateStructure structure, JrxmlBuilder.BuildContext ctx) {
    Map<String, String> result = new HashMap<>();
    result.put("uz", JrxmlBuilder.build(structure, ctx, "uz"));
    result.put("ru", JrxmlBuilder.build(structure, ctx, "ru"));
    result.put("en", JrxmlBuilder.build(structure, ctx, "en"));
    return result;
  }

  // Berilgan tilda (uz/ru/en) shablon strukturasidan JRXML quradi — render vaqtida
  // ko'rish tiliga mos hujjat uchun. Struktura yo'q (raw JRXML shablon) bo'lsa null.
  public String buildJrxmlForLang(
      final uz.hesap.service.document.domain.template.TemplateEntity e, final String lang) {
    if (e.getTemplateStructure() == null || e.getTemplateStructure().isBlank()) {
      return null;
    }
    return JrxmlBuilder.build(parse(e.getTemplateStructure()), ctxOf(e), lang);
  }

  // Shablon entity'sidan JrxmlBuilder konteksti: sarlavha = name*, 1/2-taraf = first/secondName*.
  private static JrxmlBuilder.BuildContext ctxOf(
      uz.hesap.service.document.domain.template.TemplateEntity e) {
    return new JrxmlBuilder.BuildContext(
        e.getNameUz(), e.getNameRu(), e.getNameEn(),
        e.getSellerNameUz(), e.getSellerNameRu(), e.getSellerNameEn(),
        e.getBuyerNameUz(), e.getBuyerNameRu(), e.getBuyerNameEn(),
        e.getProductNameUz(), e.getProductNameRu(), e.getProductNameEn());
  }

  /** Strukturadan JRXML yasab kompilyatsiya qiladi (saqlamasdan). */
  public Mono<CompileCheckResponse> compileCheck(final String structureJson) {
    return Mono.fromCallable(
            () -> {
              String jrxml = JrxmlBuilder.build(parse(structureJson));
              try (var in =
                  new java.io.ByteArrayInputStream(
                      jrxml.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                JasperCompileManager.compileReport(in);
                return new CompileCheckResponse(true, "OK");
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(
            e -> {
              log.warn("compile-check xato: {}", e.getMessage());
              return Mono.just(new CompileCheckResponse(false, rootMessage(e)));
            });
  }

  // ---- internals -----------------------------------------------------------

  private TemplateStructure parse(String json) {
    if (json == null || json.isBlank()) {
      throw new BadRequestException("Struktura bo'sh");
    }
    try {
      return mapper().readValue(json, TemplateStructure.class);
    } catch (Exception e) {
      throw new BadRequestException("Struktura JSON xato: " + e.getMessage());
    }
  }

  // Strukturadan field'larni ajratib, mavjudlari bilan ID'ni bog'lab upsert qiladi.
  private Mono<List<TemplateFieldResponse>> syncFields(UUID templateId, String structureJson) {
    List<TemplateFieldRequest> requested = StructureFieldSync.toRequests(parse(structureJson));
    if (requested.isEmpty()) {
      // Struktura field yaratmaydi — mavjud struktura-field'larni tozalaymiz.
      return templateFieldRepository
          .softDeleteNotIn(templateId, new UUID[0])
          .thenMany(templateFieldRepository.findAllByTemplateIdAndDeletedFalse(templateId))
          .map(TemplateFieldMapper.INSTANCE::toResponse)
          .collectList();
    }
    return templateFieldRepository
        .findAllByTemplateIdAndDeletedFalse(templateId)
        .collectList()
        .flatMapMany(existing -> templateFieldService.upsert(templateId, mergeIds(existing, requested)))
        .collectList();
  }

  // Mavjud field'lar (parentKey|keyName) bo'yicha ID'ni request'ga ko'chiradi (qayta yaratmaslik uchun).
  private List<TemplateFieldRequest> mergeIds(
      List<TemplateFieldEntity> existing, List<TemplateFieldRequest> requested) {
    Map<String, UUID> byKey = new HashMap<>();
    for (TemplateFieldEntity e : existing) {
      byKey.put(key(e.getParentKey(), e.getKeyName()), e.getId());
    }
    return requested.stream()
        .map(
            r -> {
              UUID id = byKey.get(key(r.parentKey(), r.keyName()));
              if (id == null) {
                return r;
              }
              return new TemplateFieldRequest(
                  id, r.parentKey(), r.templateId(), r.nameUz(), r.nameRu(), r.nameEn(), r.keyName(),
                  r.type(), r.position(), r.productField());
            })
        .toList();
  }

  private static String key(String parentKey, String keyName) {
    return (parentKey == null ? "" : parentKey) + "|" + (keyName == null ? "" : keyName);
  }

  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    String m = t.getMessage();
    return m == null ? t.getClass().getSimpleName() : m;
  }
}
