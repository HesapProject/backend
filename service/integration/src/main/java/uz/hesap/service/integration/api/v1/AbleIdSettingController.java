package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.ableid.AbleIdSettingRequest;
import uz.hesap.service.integration.model.ableid.AbleIdSettingResponse;
import uz.hesap.service.integration.service.AbleIdSettingService;

@RestController
@RequestMapping("/integration/v1/able-id/settings")
@RequiredArgsConstructor
public class AbleIdSettingController {

  private final AbleIdSettingService service;

  @GetMapping
  public Mono<AbleIdSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<AbleIdSettingResponse> createOrUpdate(@RequestBody AbleIdSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
