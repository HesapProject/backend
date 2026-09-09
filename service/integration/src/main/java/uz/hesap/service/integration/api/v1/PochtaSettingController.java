package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.PochtaLogResponse;
import uz.hesap.service.integration.model.PochtaSettingRequest;
import uz.hesap.service.integration.model.PochtaSettingResponse;
import uz.hesap.service.integration.model.PochtaTestResult;
import uz.hesap.service.integration.service.pochta.PochtaService;
import uz.hesap.service.integration.service.pochta.PochtaSettingService;

// hybrid.pochta.uz integratsiyasi — credential settings + monitoring loglar.
@RestController
@RequestMapping("/integration/v1/pochta")
@RequiredArgsConstructor
public class PochtaSettingController {

  private final PochtaSettingService settingService;
  private final PochtaService pochtaService;

  @GetMapping("/settings")
  public Mono<PochtaSettingResponse> get() {
    return settingService.get();
  }

  @PostMapping("/settings")
  public Mono<PochtaSettingResponse> createOrUpdate(@RequestBody PochtaSettingRequest request) {
    return settingService.createOrUpdate(request);
  }

  // Ulanishni tekshirish (token olishga urinadi) — natija log'ga ham yoziladi.
  @PostMapping("/test")
  public Mono<PochtaTestResult> test() {
    return pochtaService.testConnection();
  }

  // Monitoring loglari (eng yangi birinchi).
  @GetMapping("/logs")
  public Flux<PochtaLogResponse> logs(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return pochtaService.logs(page, size);
  }
}
