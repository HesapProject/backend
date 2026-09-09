package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.OneIdSettingRequest;
import uz.hesap.service.integration.model.OneIdSettingResponse;
import uz.hesap.service.integration.service.OneIdSettingService;

@RestController
@RequestMapping("/integration/v1/oneid/settings")
@RequiredArgsConstructor
public class OneIdSettingController {

  private final OneIdSettingService service;

  @GetMapping
  public Mono<OneIdSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<OneIdSettingResponse> createOrUpdate(@RequestBody OneIdSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
