package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.AbleIdSettingEntity;
import uz.hesap.service.integration.model.ableid.AbleIdSettingRequest;
import uz.hesap.service.integration.model.ableid.AbleIdSettingResponse;
import uz.hesap.service.integration.repository.AbleIdSettingRepository;

// AbleID credential'lari — PlumSettingService bilan bir xil singleton pattern.
// DB qatori bo'sh bo'lsa application.yml'dagi qiymatlarga qaytadi.
@Service
@Log4j2
public class AbleIdSettingService {

  private final AbleIdSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultProjectId;
  private final String defaultSecret;
  private final String defaultHookUrl;

  public AbleIdSettingService(
      AbleIdSettingRepository repository,
      @Value("${able-id.base-url:https://faceid-back.theable.tech}") String defaultBaseUrl,
      @Value("${able-id.project-id:}") String defaultProjectId,
      @Value("${able-id.secret:}") String defaultSecret,
      @Value("${able-id.hook-url:https://api.business.hesap.uz/integration/v1/able-id/hook}")
          String defaultHookUrl) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultProjectId = defaultProjectId;
    this.defaultSecret = defaultSecret;
    this.defaultHookUrl = defaultHookUrl;
  }

  // AbleIdService uchun aktual creds: DB → fallback yml.
  public Mono<AbleIdSettingEntity> getCurrent() {
    return repository.findAll().next().switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  // Admin UI'ga: secret yashirin.
  public Mono<AbleIdSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton.
  public Mono<AbleIdSettingResponse> createOrUpdate(AbleIdSettingRequest request) {
    log.debug("Save AbleID setting: projectId={}", request.projectId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new AbleIdSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setProjectId(request.projectId());
              if (request.secret() != null && !request.secret().isBlank()) {
                existing.setSecret(request.secret());
              }
              if (request.hookUrl() != null && !request.hookUrl().isBlank()) {
                existing.setHookUrl(request.hookUrl());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private AbleIdSettingResponse toResponse(AbleIdSettingEntity entity) {
    boolean secretSet = entity.getSecret() != null && !entity.getSecret().isBlank();
    String hook =
        entity.getHookUrl() == null || entity.getHookUrl().isBlank()
            ? defaultHookUrl
            : entity.getHookUrl();
    return new AbleIdSettingResponse(entity.getBaseUrl(), entity.getProjectId(), secretSet, hook);
  }

  private AbleIdSettingEntity fromYml() {
    AbleIdSettingEntity e = new AbleIdSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setProjectId(defaultProjectId);
    e.setSecret(defaultSecret);
    e.setHookUrl(defaultHookUrl);
    return e;
  }
}
