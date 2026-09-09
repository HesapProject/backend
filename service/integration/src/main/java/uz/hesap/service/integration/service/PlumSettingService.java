package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PlumSettingEntity;
import uz.hesap.service.integration.model.PlumSettingRequest;
import uz.hesap.service.integration.model.PlumSettingResponse;
import uz.hesap.service.integration.repository.PlumSettingRepository;

// Admin UI orqali Plum credential'larini boshqarish + PlumService/Scheduler
// uchun runtime'da o'qish. Settings singleton — bitta qator saqlanadi.
//
// DB qatori bo'sh bo'lsa application.yml'dagi qiymatlarga qaytadi
// (backward compatibility).
@Service
@Log4j2
public class PlumSettingService {

  private final PlumSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultLogin;
  private final String defaultPassword;

  public PlumSettingService(
      PlumSettingRepository repository,
      @Value("${plum.base-url:}") String defaultBaseUrl,
      @Value("${plum.login:}") String defaultLogin,
      @Value("${plum.password:}") String defaultPassword) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultLogin = defaultLogin;
    this.defaultPassword = defaultPassword;
  }

  // PlumService uchun aktual creds: DB → fallback yml.
  public Mono<PlumSettingEntity> getCurrent() {
    return repository.findAll().next().switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  // Admin UI'ga: secret yashirin.
  public Mono<PlumSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton.
  public Mono<PlumSettingResponse> createOrUpdate(PlumSettingRequest request) {
    log.debug("Save Plum setting: login={}", request.login());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new PlumSettingEntity())
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

  private PlumSettingResponse toResponse(PlumSettingEntity entity) {
    boolean passwordSet = entity.getPassword() != null && !entity.getPassword().isBlank();
    return new PlumSettingResponse(entity.getBaseUrl(), entity.getLogin(), passwordSet);
  }

  private PlumSettingEntity fromYml() {
    PlumSettingEntity e = new PlumSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setLogin(defaultLogin);
    e.setPassword(defaultPassword);
    return e;
  }
}
