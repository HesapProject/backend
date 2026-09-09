package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.PaymeSettingRequest;
import uz.hesap.service.integration.model.PaymeSettingResponse;
import uz.hesap.service.integration.service.PaymeSettingService;

@RestController
@RequestMapping("/integration/v1/payme/settings")
@RequiredArgsConstructor
public class PaymeSettingController {

  private final PaymeSettingService service;

  @GetMapping
  public Mono<PaymeSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<PaymeSettingResponse> createOrUpdate(@RequestBody PaymeSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
