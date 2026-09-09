package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.ClickSettingRequest;
import uz.hesap.service.integration.model.ClickSettingResponse;
import uz.hesap.service.integration.service.ClickSettingService;

@RestController
@RequestMapping("/integration/v1/click/settings")
@RequiredArgsConstructor
public class ClickSettingController {

  private final ClickSettingService service;

  @GetMapping
  public Mono<ClickSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<ClickSettingResponse> createOrUpdate(@RequestBody ClickSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
