package uz.hesap.service.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.OneIdSettingEntity;
import uz.hesap.service.integration.model.OneIdSettingRequest;
import uz.hesap.service.integration.model.OneIdSettingResponse;
import uz.hesap.service.integration.repository.OneIdSettingRepository;

// OneID credential'larini singleton row sifatida boshqaradi.
@Service
@RequiredArgsConstructor
@Log4j2
public class OneIdSettingService {

  private final OneIdSettingRepository repository;

  public Mono<OneIdSettingEntity> getCurrent() {
    return repository.findAll().next();
  }

  public Mono<OneIdSettingResponse> get() {
    return repository
        .findAll()
        .next()
        .map(this::toResponse)
        .defaultIfEmpty(new OneIdSettingResponse(null, null, false));
  }

  public Mono<OneIdSettingResponse> createOrUpdate(OneIdSettingRequest request) {
    log.debug("Save OneID setting: clientId={}", request.clientId());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new OneIdSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              existing.setClientId(request.clientId());
              if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
                existing.setClientSecret(request.clientSecret());
              }
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private OneIdSettingResponse toResponse(OneIdSettingEntity entity) {
    boolean secretSet = entity.getClientSecret() != null && !entity.getClientSecret().isBlank();
    return new OneIdSettingResponse(entity.getBaseUrl(), entity.getClientId(), secretSet);
  }
}
