package uz.hesap.service.integration.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.context.MyIdProperties;
import uz.hesap.service.integration.domain.MyIdSettingEntity;
import uz.hesap.service.integration.model.MyIdSettingRequest;
import uz.hesap.service.integration.model.MyIdSettingResponse;
import uz.hesap.service.integration.repository.MyIdSettingRepository;

// Admin UI orqali MyID credential'larini boshqarish + MyIdService uchun
// runtime'da o'qish. Settings singleton — bitta qator saqlanadi.
@Service
@Log4j2
public class MyIdSettingService {

  // Har platforma uchun alohida OAuth token kesh kaliti.
  private static final String MY_ID_TOKEN_MOBILE = "MY_ID_TOKEN_MOBILE";
  private static final String MY_ID_TOKEN_WEB = "MY_ID_TOKEN_WEB";

  private final MyIdSettingRepository repository;
  private final MyIdProperties myIdProperties;
  private final Cache<String, String> cachedMyIdToken;

  // Explicit konstruktor — `@Qualifier` parametrga aniq qo'yiladi
  // (Cache<String, String> bean ko'p, disambiguatsiya kerak).
  public MyIdSettingService(
      MyIdSettingRepository repository,
      MyIdProperties myIdProperties,
      @Qualifier("cachedMyIdToken") Cache<String, String> cachedMyIdToken) {
    this.repository = repository;
    this.myIdProperties = myIdProperties;
    this.cachedMyIdToken = cachedMyIdToken;
  }

  // MyIdService uchun: DB'da setting bo'lsa shuni, bo'lmasa application.yml'dagi
  // default qiymatni qaytaradi. Shu sababli admin DB'ga yozmasdan ham xizmat
  // ishlaydi (eski deploylar buzilmaydi).
  public Mono<MyIdSettingEntity> getCurrent() {
    return repository
        .findAll()
        .next()
        .switchIfEmpty(Mono.fromSupplier(this::fromProperties));
  }

  // Admin UI'ga qaytaradigan javob — secret yashirin.
  public Mono<MyIdSettingResponse> get() {
    return getCurrent().map(this::toResponse);
  }

  // CreateOrUpdate — singleton. Har bir secret bo'sh kelsa avvalgi qiymat saqlanadi.
  public Mono<MyIdSettingResponse> createOrUpdate(MyIdSettingRequest request) {
    log.debug(
        "Save MyID setting: mobileClientId={}, webClientId={}",
        request.mobileClientId(),
        request.webClientId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new MyIdSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setMobileClientId(request.mobileClientId());
              existing.setWebClientId(request.webClientId());
              if (isPresent(request.mobileClientSecret())) {
                existing.setMobileClientSecret(request.mobileClientSecret());
              }
              if (isPresent(request.webClientSecret())) {
                existing.setWebClientSecret(request.webClientSecret());
              }
              return repository.save(existing);
            })
        // Settings o'zgardi — har ikkala OAuth token kesh endi yaroqsiz.
        .doOnSuccess(
            saved -> {
              cachedMyIdToken.invalidate(MY_ID_TOKEN_MOBILE);
              cachedMyIdToken.invalidate(MY_ID_TOKEN_WEB);
            })
        .map(this::toResponse);
  }

  private MyIdSettingResponse toResponse(MyIdSettingEntity entity) {
    return new MyIdSettingResponse(
        entity.getBaseUrl(),
        entity.getMobileClientId(),
        isPresent(entity.getMobileClientSecret()),
        entity.getWebClientId(),
        isPresent(entity.getWebClientSecret()));
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isBlank();
  }

  // application.yml'dagi default qiymatlardan entity yasaydi (DB bo'sh holatda).
  private MyIdSettingEntity fromProperties() {
    MyIdSettingEntity e = new MyIdSettingEntity();
    e.setBaseUrl(myIdProperties.getBaseUrl());
    e.setMobileClientId(myIdProperties.getMobileClientId());
    e.setMobileClientSecret(myIdProperties.getMobileClientSecret());
    e.setWebClientId(myIdProperties.getWebClientId());
    e.setWebClientSecret(myIdProperties.getWebClientSecret());
    return e;
  }
}
