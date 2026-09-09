package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.RoumingSettingEntity;
import uz.hesap.service.integration.model.RoumingSettingRequest;
import uz.hesap.service.integration.model.RoumingSettingResponse;
import uz.hesap.service.integration.repository.RoumingSettingRepository;

// Admin UI orqali Factura.uz credential'larini boshqarish + RoumingService uchun
// runtime'da o'qish. Singleton — bitta qator. DB bo'sh bo'lsa application.yml'ga qaytadi.
@Service
@Log4j2
public class RoumingSettingService {

  private final RoumingSettingRepository repository;
  private final String defaultBaseUrl;
  private final String defaultTokenUrl;
  private final String defaultLogin;
  private final String defaultPassword;
  private final String defaultClientId;
  private final String defaultClientSecret;

  public RoumingSettingService(
      RoumingSettingRepository repository,
      @Value("${rouming.base-url:}") String defaultBaseUrl,
      @Value("${rouming.token-url:}") String defaultTokenUrl,
      @Value("${rouming.login:}") String defaultLogin,
      @Value("${rouming.password:}") String defaultPassword,
      @Value("${rouming.client-id:}") String defaultClientId,
      @Value("${rouming.client-secret:}") String defaultClientSecret) {
    this.repository = repository;
    this.defaultBaseUrl = defaultBaseUrl;
    this.defaultTokenUrl = defaultTokenUrl;
    this.defaultLogin = defaultLogin;
    this.defaultPassword = defaultPassword;
    this.defaultClientId = defaultClientId;
    this.defaultClientSecret = defaultClientSecret;
  }

  // RoumingService uchun aktual creds: DB → yml fallback. DB'da bo'sh qolgan
  // maydonlar yml qiymati bilan to'ldiriladi (masalan token-url ko'pincha yml'da).
  public Mono<RoumingSettingEntity> getCurrent() {
    return repository
        .findAll()
        .next()
        .map(this::withYmlFallback)
        .switchIfEmpty(Mono.fromSupplier(this::fromYml));
  }

  // Admin UI'ga: parol/secret yashirin.
  public Mono<RoumingSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton. password/clientSecret bo'sh kelsa eski qiymat saqlanadi.
  public Mono<RoumingSettingResponse> createOrUpdate(RoumingSettingRequest request) {
    log.debug("Save Factura.uz setting: login={}, clientId={}", request.login(), request.clientId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new RoumingSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setTokenUrl(request.tokenUrl());
              existing.setLogin(request.login());
              existing.setClientId(request.clientId());
              if (notBlank(request.password())) {
                existing.setPassword(request.password());
              }
              if (notBlank(request.clientSecret())) {
                existing.setClientSecret(request.clientSecret());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private RoumingSettingResponse toResponse(RoumingSettingEntity e) {
    return new RoumingSettingResponse(
        e.getBaseUrl(),
        e.getTokenUrl(),
        e.getLogin(),
        notBlank(e.getPassword()),
        e.getClientId(),
        notBlank(e.getClientSecret()));
  }

  // DB qatoridagi bo'sh maydonlarni yml default bilan to'ldiradi.
  private RoumingSettingEntity withYmlFallback(RoumingSettingEntity e) {
    if (!notBlank(e.getBaseUrl())) e.setBaseUrl(defaultBaseUrl);
    if (!notBlank(e.getTokenUrl())) e.setTokenUrl(defaultTokenUrl);
    if (!notBlank(e.getLogin())) e.setLogin(defaultLogin);
    if (!notBlank(e.getPassword())) e.setPassword(defaultPassword);
    if (!notBlank(e.getClientId())) e.setClientId(defaultClientId);
    if (!notBlank(e.getClientSecret())) e.setClientSecret(defaultClientSecret);
    return e;
  }

  private RoumingSettingEntity fromYml() {
    RoumingSettingEntity e = new RoumingSettingEntity();
    e.setBaseUrl(defaultBaseUrl);
    e.setTokenUrl(defaultTokenUrl);
    e.setLogin(defaultLogin);
    e.setPassword(defaultPassword);
    e.setClientId(defaultClientId);
    e.setClientSecret(defaultClientSecret);
    return e;
  }

  private static boolean notBlank(String v) {
    return v != null && !v.isBlank();
  }
}
