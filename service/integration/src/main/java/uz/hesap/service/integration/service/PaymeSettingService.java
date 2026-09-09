package uz.hesap.service.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PaymeSettingEntity;
import uz.hesap.service.integration.model.PaymeSettingRequest;
import uz.hesap.service.integration.model.PaymeSettingResponse;
import uz.hesap.service.integration.repository.PaymeSettingRepository;

// Payme kredensiallarini admin UI orqali boshqarish + PaymeService runtime'da
// o'qish. Settings singleton — bitta qator saqlanadi (PlumSettingService kabi).
@Service
@Log4j2
@RequiredArgsConstructor
public class PaymeSettingService {

  private final PaymeSettingRepository repository;

  // PaymeService uchun aktual creds (DB qatori bo'lmasa empty).
  public Mono<PaymeSettingEntity> getCurrent() {
    return repository.findAll().next();
  }

  // Admin UI'ga: secret yashirin, qator bo'lmasa bo'sh javob.
  public Mono<PaymeSettingResponse> get() {
    return getCurrent().map(this::toResponse).defaultIfEmpty(new PaymeSettingResponse(null, false));
  }

  public Mono<PaymeSettingResponse> createOrUpdate(PaymeSettingRequest request) {
    log.debug("Save Payme setting: merchantId={}", request.merchantId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new PaymeSettingEntity())
        .flatMap(
            existing -> {
              existing.setMerchantId(request.merchantId());
              if (request.secret() != null && !request.secret().isBlank()) {
                existing.setSecret(request.secret());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private PaymeSettingResponse toResponse(PaymeSettingEntity entity) {
    boolean secretSet = entity.getSecret() != null && !entity.getSecret().isBlank();
    return new PaymeSettingResponse(entity.getMerchantId(), secretSet);
  }
}
