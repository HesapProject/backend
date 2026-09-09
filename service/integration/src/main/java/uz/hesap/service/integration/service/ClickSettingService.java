package uz.hesap.service.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.ClickSettingEntity;
import uz.hesap.service.integration.model.ClickSettingRequest;
import uz.hesap.service.integration.model.ClickSettingResponse;
import uz.hesap.service.integration.repository.ClickSettingRepository;

// Click kredensiallarini admin UI orqali boshqarish + ClickService runtime'da
// o'qish. Settings singleton — bitta qator saqlanadi (PlumSettingService kabi).
@Service
@Log4j2
@RequiredArgsConstructor
public class ClickSettingService {

  private final ClickSettingRepository repository;

  // ClickService uchun aktual creds (DB qatori bo'lmasa empty).
  public Mono<ClickSettingEntity> getCurrent() {
    return repository.findAll().next();
  }

  // Admin UI'ga: secretKey yashirin, qator bo'lmasa bo'sh javob.
  public Mono<ClickSettingResponse> get() {
    return getCurrent()
        .map(this::toResponse)
        .defaultIfEmpty(new ClickSettingResponse(null, null, false));
  }

  public Mono<ClickSettingResponse> createOrUpdate(ClickSettingRequest request) {
    log.debug("Save Click setting: serviceId={}, merchantId={}", request.serviceId(), request.merchantId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new ClickSettingEntity())
        .flatMap(
            existing -> {
              existing.setServiceId(request.serviceId());
              existing.setMerchantId(request.merchantId());
              if (request.secretKey() != null && !request.secretKey().isBlank()) {
                existing.setSecretKey(request.secretKey());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private ClickSettingResponse toResponse(ClickSettingEntity entity) {
    boolean keySet = entity.getSecretKey() != null && !entity.getSecretKey().isBlank();
    return new ClickSettingResponse(entity.getServiceId(), entity.getMerchantId(), keySet);
  }
}
