package uz.hesap.service.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.EImzoSettingEntity;
import uz.hesap.service.integration.model.EImzoSettingRequest;
import uz.hesap.service.integration.model.EImzoSettingResponse;
import uz.hesap.service.integration.repository.EImzoSettingRepository;

// E-IMZO server URL'ini singleton row sifatida boshqaradi.
@Service
@RequiredArgsConstructor
@Log4j2
public class EImzoSettingService {

  private final EImzoSettingRepository repository;

  public Mono<EImzoSettingEntity> getCurrent() {
    return repository.findAll().next();
  }

  public Mono<EImzoSettingResponse> get() {
    return repository
        .findAll()
        .next()
        .map(this::toResponse)
        .defaultIfEmpty(new EImzoSettingResponse(null));
  }

  public Mono<EImzoSettingResponse> createOrUpdate(EImzoSettingRequest request) {
    log.debug("Save E-IMZO setting: baseUrl={}", request.baseUrl());
    return repository
        .findAll()
        .next()
        .defaultIfEmpty(new EImzoSettingEntity())
        .flatMap(
            existing -> {
              existing.setBaseUrl(request.baseUrl());
              return repository.save(existing);
            })
        .map(this::toResponse);
  }

  private EImzoSettingResponse toResponse(EImzoSettingEntity entity) {
    return new EImzoSettingResponse(entity.getBaseUrl());
  }
}
