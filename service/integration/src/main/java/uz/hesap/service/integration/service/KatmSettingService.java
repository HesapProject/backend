package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.KatmSettingEntity;
import uz.hesap.service.integration.model.KatmSettingRequest;
import uz.hesap.service.integration.model.KatmSettingResponse;
import uz.hesap.service.integration.repository.KatmSettingRepository;

// Admin UI orqali KATM credential'larini boshqarish + KatmTokenService/KatmService
// uchun runtime'da o'qish. Settings singleton — bitta qator saqlanadi.
// DB qatori bo'sh bo'lsa application.yml'dagi qiymatlarga qaytadi.
@Service
@Log4j2
public class KatmSettingService {

  private final KatmSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultLogin;
  private final String defaultPassword;

  public KatmSettingService(
      KatmSettingRepository repository,
      @Value("${katm.base-url:}") String defaultBaseUrl,
      @Value("${katm.login:}") String defaultLogin,
      @Value("${katm.password:}") String defaultPassword) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultLogin = defaultLogin;
    this.defaultPassword = defaultPassword;
  }

  // KatmService uchun aktual creds: DB → fallback yml.
  public Mono<KatmSettingEntity> getCurrent() {
    return repository.findAll().next().switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  // Admin UI'ga: secret yashirin.
  public Mono<KatmSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton.
  public Mono<KatmSettingResponse> createOrUpdate(KatmSettingRequest request) {
    log.debug("Save KATM setting: login={}", request.login());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new KatmSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setLogin(request.login());
              if (request.password() != null && !request.password().isBlank()) {
                existing.setPassword(request.password());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private KatmSettingResponse toResponse(KatmSettingEntity entity) {
    boolean passwordSet = entity.getPassword() != null && !entity.getPassword().isBlank();
    return new KatmSettingResponse(entity.getBaseUrl(), entity.getLogin(), passwordSet);
  }

  private KatmSettingEntity fromYml() {
    KatmSettingEntity e = new KatmSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setLogin(defaultLogin);
    e.setPassword(defaultPassword);
    return e;
  }
}
