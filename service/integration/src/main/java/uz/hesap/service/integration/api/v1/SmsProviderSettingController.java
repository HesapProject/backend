package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.SmsProviderSettingRequest;
import uz.hesap.service.integration.model.SmsProviderSettingResponse;
import uz.hesap.service.integration.service.SmsProviderSettingService;

@RestController
@RequestMapping("/integration/v1/eskiz/settings")
@RequiredArgsConstructor
public class SmsProviderSettingController {

  private final SmsProviderSettingService smsProviderSettingService;

  @PostMapping
  public Mono<SmsProviderSettingResponse> createOrUpdate(
      @RequestBody SmsProviderSettingRequest request) {
    return smsProviderSettingService.createOrUpdate(request);
  }

  @GetMapping
  public Mono<SmsProviderSettingResponse> get() {
    return smsProviderSettingService.get();
  }
}
