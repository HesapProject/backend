package uz.hesap.service.integration.service.pochta;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PochtaSettingEntity;
import uz.hesap.service.integration.model.PochtaSettingRequest;
import uz.hesap.service.integration.model.PochtaSettingResponse;
import uz.hesap.service.integration.repository.PochtaSettingRepository;

// Admin UI orqali hybrid.pochta.uz credential'larini boshqarish + PochtaService uchun
// runtime'da o'qish. Singleton — bitta qator. DB bo'sh bo'lsa yml'ga qaytadi.
@Service
@Log4j2
public class PochtaSettingService {

  private final PochtaSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultUsername;
  private final String defaultPassword;

  public PochtaSettingService(
      PochtaSettingRepository repository,
      @Value("${pochta.base-url:https://hybrid.pochta.uz/}") String defaultBaseUrl,
      @Value("${pochta.username:}") String defaultUsername,
      @Value("${pochta.password:}") String defaultPassword) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultUsername = defaultUsername;
    this.defaultPassword = defaultPassword;
  }

  public Mono<PochtaSettingEntity> getCurrent() {
    return repository.findAll().next().switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  public Mono<PochtaSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  public Mono<PochtaSettingResponse> createOrUpdate(PochtaSettingRequest request) {
    log.debug("Save Pochta setting: username={}", request.username());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new PochtaSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setUsername(request.username());
              if (request.password() != null && !request.password().isBlank()) {
                existing.setPassword(request.password());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private PochtaSettingResponse toResponse(PochtaSettingEntity e) {
    boolean set = e.getPassword() != null && !e.getPassword().isBlank();
    return new PochtaSettingResponse(e.getBaseUrl(), e.getUsername(), set);
  }

  private PochtaSettingEntity fromYml() {
    PochtaSettingEntity e = new PochtaSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setUsername(defaultUsername);
    e.setPassword(defaultPassword);
    return e;
  }
}
