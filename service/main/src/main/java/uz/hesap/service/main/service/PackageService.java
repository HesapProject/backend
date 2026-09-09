package uz.hesap.service.main.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.PackageEntity;
import uz.hesap.service.main.model.PackageRequest;
import uz.hesap.service.main.model.PackageResponse;
import uz.hesap.service.main.model.mapper.PackageMapper;
import uz.hesap.service.main.model.tariff.TariffTemplateConfig;
import uz.hesap.service.main.model.tariff.TariffTemplateRequest;
import uz.hesap.service.main.repository.PackageRepository;
import uz.hesap.service.main.feign.DocumentServiceClient;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.*;
import uz.hesap.service.common.util.enums.TariffType;

@Service
@RequiredArgsConstructor
@Log4j2
public class PackageService {

  private final PackageRepository packageRepository;
  private final PackageMapper packageMapper;
  private final ObjectMapper objectMapper;
  private final DocumentServiceClient documentServiceClient;

  private static final String PKG_NOT_FOUND = "Package not found with id: ";

  // --- CRUD ---

  public Mono<PackageResponse> create(final PackageRequest request) {
    PackageEntity entity = packageMapper.toEntity(request);
    entity.setTemplates(serializeConfig(request.templateConfig()));
    return packageRepository.save(entity).flatMap(this::enrichSingle);
  }

  public Mono<PackageResponse> update(final UUID id, final PackageRequest request) {
    return packageRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(PKG_NOT_FOUND + id)))
        .flatMap(
            entity -> {
              packageMapper.toUpdate(request, entity);
              entity.setTemplates(serializeConfig(request.templateConfig()));
              return packageRepository.save(entity);
            })
        .flatMap(this::enrichSingle);
  }

  public Mono<Void> delete(final UUID id) {
    return packageRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(PKG_NOT_FOUND + id)))
        .flatMap(
            p -> {
              p.setDeleted(Boolean.TRUE);
              return packageRepository.save(p);
            })
        .then();
  }

  public Mono<PackageResponse> getById(final UUID id) {
    return packageRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(PKG_NOT_FOUND + id)))
        .flatMap(this::enrichSingle);
  }

  public Flux<PackageResponse> getAll(TariffType type) {
    Flux<PackageEntity> flux =
        (type != null)
            ? packageRepository.findAllByTypeAndDeletedFalseOrderByCreatedDateDesc(type)
            : packageRepository.findAllByDeletedFalseOrderByCreatedDateDesc();
    return flux.collectList().flatMapMany(this::enrichList);
  }

  public Mono<Page<PackageResponse>> getAllPaged(TariffType type, Pageable pageable) {
    Flux<PackageEntity> flux =
        (type != null)
            ? packageRepository.findAllByTypeAndDeletedFalseOrderByCreatedDateDesc(type, pageable)
            : packageRepository.findAllByDeletedFalseOrderByCreatedDateDesc(pageable);
    Mono<Long> countMono =
        (type != null)
            ? packageRepository.countByTypeAndDeletedFalse(type)
            : packageRepository.countByDeletedFalse();

    return flux.collectList()
        .flatMap(
            entities -> {
              if (entities.isEmpty()) {
                return Mono.zip(Mono.just(Collections.<PackageResponse>emptyList()), countMono);
              }
              return Mono.zip(enrichList(entities).collectList(), countMono);
            })
        .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  // --- enrich ---
  private Mono<PackageResponse> enrichSingle(PackageEntity entity) {
    return enrichList(List.of(entity)).next();
  }

  private Flux<PackageResponse> enrichList(List<PackageEntity> entities) {
    if (entities.isEmpty()) return Flux.empty();

    Map<UUID, TariffTemplateConfig> configMap =
        entities.stream().collect(Collectors.toMap(PackageEntity::getId, this::parseConfigSafe));

    List<UUID> allTemplateIds =
        configMap.values().stream()
            .filter(c -> c.templates() != null)
            .flatMap(c -> c.templates().stream())
            .map(TariffTemplateRequest::templateId)
            .distinct()
            .collect(Collectors.toList());

    if (allTemplateIds.isEmpty()) {
      return Flux.fromIterable(entities)
          .map(e -> toResponse(e, configMap.get(e.getId()), Collections.emptyMap()));
    }

    return documentServiceClient
        .getTemplateNamesMap(allTemplateIds)
        .defaultIfEmpty(Collections.emptyMap())
        .flatMapMany(
            templateMap ->
                Flux.fromIterable(entities)
                    .map(e -> toResponse(e, configMap.get(e.getId()), templateMap)));
  }

  private PackageResponse toResponse(
      PackageEntity entity,
      TariffTemplateConfig config,
      Map<UUID, TemplateBasicResponse> templateMap) {

    List<TariffTemplateResponse> templateResponses = Collections.emptyList();
    if (config.templates() != null) {
      templateResponses =
          config.templates().stream()
              .map(t -> new TariffTemplateResponse(templateMap.get(t.templateId()), t.count()))
              .filter(t -> t.template() != null)
              .collect(Collectors.toList());
    }

    TemplateConfigResponse configResponse =
        new TemplateConfigResponse(config.type(), config.totalCount(), templateResponses);

    return new PackageResponse(
        entity.getId(),
        new TextModel(entity.getNameUz(), entity.getNameRu(), entity.getNameEn()),
        new TextModel(
            entity.getDescriptionUz(), entity.getDescriptionRu(), entity.getDescriptionEn()),
        entity.getPrice(),
        entity.getStars(),
        entity.getDuration(),
        entity.getType(),
        configResponse,
        nz(entity.getScoringHesap()),
        nz(entity.getScoringKatm()),
        nz(entity.getScoringPayment()),
        entity.getCreatedDate(),
        entity.getLastModifiedDate());
  }

  // --- JSON ---

  private static Integer nz(Integer v) {
    return v == null ? 0 : v;
  }

  private String serializeConfig(TariffTemplateConfig config) {
    if (config == null) return null;
    try {
      return objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error serializing template config", e);
    }
  }

  private TariffTemplateConfig parseConfigSafe(PackageEntity entity) {
    if (entity.getTemplates() == null || entity.getTemplates().isEmpty()) {
      return new TariffTemplateConfig(null, null, Collections.emptyList());
    }
    try {
      return objectMapper.readValue(entity.getTemplates(), TariffTemplateConfig.class);
    } catch (JsonProcessingException e) {
      log.error("Error parsing template config for package {}", entity.getId(), e);
      return new TariffTemplateConfig(null, null, Collections.emptyList());
    }
  }
}
