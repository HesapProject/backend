package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.TuranixSettingEntity;
import uz.hesap.service.integration.model.TuranixSettingRequest;
import uz.hesap.service.integration.model.TuranixSettingResponse;
import uz.hesap.service.integration.repository.TuranixSettingRepository;

// Admin UI orqali Turanix credential'larini boshqarish + TuranixService uchun
// runtime'da o'qish. Settings singleton — bitta qator saqlanadi. DB qatori bo'sh
// bo'lsa application.yml'dagi qiymatlarga qaytadi. KatmSettingService bilan bir xil.
@Service
@Log4j2
public class TuranixSettingService {

  private final TuranixSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultProjectId;
  private final String defaultSecretKey;

  public TuranixSettingService(
      TuranixSettingRepository repository,
      @Value("${turanix.base-url:}") String defaultBaseUrl,
      @Value("${turanix.project-id:}") String defaultProjectId,
      @Value("${turanix.secret-key:}") String defaultSecretKey) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultProjectId = defaultProjectId;
    this.defaultSecretKey = defaultSecretKey;
  }

  // TuranixService uchun aktual creds: DB → fallback yml.
  public Mono<TuranixSettingEntity> getCurrent() {
    return repository.findAll().next().switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  // Admin UI'ga: secret yashirin.
  public Mono<TuranixSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton.
  public Mono<TuranixSettingResponse> createOrUpdate(TuranixSettingRequest request) {
    log.debug("Save Turanix setting: projectId={}", request.projectId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new TuranixSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setProjectId(request.projectId());
              if (request.secretKey() != null && !request.secretKey().isBlank()) {
                existing.setSecretKey(request.secretKey());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private TuranixSettingResponse toResponse(TuranixSettingEntity entity) {
    boolean secretSet = entity.getSecretKey() != null && !entity.getSecretKey().isBlank();
    return new TuranixSettingResponse(entity.getBaseUrl(), entity.getProjectId(), secretSet);
  }

  private TuranixSettingEntity fromYml() {
    TuranixSettingEntity e = new TuranixSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setProjectId(defaultProjectId);
    e.setSecretKey(defaultSecretKey);
    return e;
  }
}
