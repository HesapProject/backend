package uz.hesap.service.integration.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.SmsProviderSettingEntity;
import uz.hesap.service.integration.model.SmsProviderSettingRequest;
import uz.hesap.service.integration.model.SmsProviderSettingResponse;
import uz.hesap.service.integration.repository.SmsProviderSettingRepository;

@Service
@AllArgsConstructor
@Log4j2
public class SmsProviderSettingService {

  private final SmsProviderSettingRepository smsProviderSettingRepository;

  // BUG FIX #1: `findAll().singleOrEmpty()` jadvalda >1 qator bo'lsa
  // IndexOutOfBoundsException (500) berardi. `.next()` birinchi qatorni oladi —
  // settings singleton bo'lgani uchun xavfsiz, ortiqcha qatorda ham crash bo'lmaydi.
  public Mono<SmsProviderSettingResponse> createOrUpdate(SmsProviderSettingRequest request) {
    log.debug("Create or update eskiz settings {}", request);
    return smsProviderSettingRepository
        .findAll()
        .next()
        .defaultIfEmpty(new SmsProviderSettingEntity())
        .flatMap(
            setting -> {
              setting.setEskizEmail(request.eskizEmail());
              setting.setEskizSecret(request.eskizSecret());
              setting.setEskizFrom(request.eskizFrom());
              return smsProviderSettingRepository.save(setting);
            })
        .map(
            setting ->
                new SmsProviderSettingResponse(
                    setting.getEskizEmail(), setting.getEskizSecret(), setting.getEskizFrom()));
  }

  public Mono<SmsProviderSettingResponse> get() {
    log.debug("Find sms provider settings");
    return smsProviderSettingRepository
        .findAll()
        .next()
        .map(
            setting ->
                new SmsProviderSettingResponse(
                    setting.getEskizEmail(), setting.getEskizSecret(), setting.getEskizFrom()))
        .defaultIfEmpty(new SmsProviderSettingResponse(null, null, null));
  }
}
