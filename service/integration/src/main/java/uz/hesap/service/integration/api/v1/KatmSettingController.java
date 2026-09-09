package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.KatmSettingRequest;
import uz.hesap.service.integration.model.KatmSettingResponse;
import uz.hesap.service.integration.service.KatmSettingService;

@RestController
@RequestMapping("/integration/v1/katm/settings")
@RequiredArgsConstructor
public class KatmSettingController {

  private final KatmSettingService service;

  @GetMapping
  public Mono<KatmSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<KatmSettingResponse> createOrUpdate(@RequestBody KatmSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
