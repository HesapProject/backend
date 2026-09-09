package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.RoumingSettingRequest;
import uz.hesap.service.integration.model.RoumingSettingResponse;
import uz.hesap.service.integration.service.RoumingSettingService;

@RestController
@RequestMapping("/integration/v1/rouming/settings")
@RequiredArgsConstructor
public class RoumingSettingController {

  private final RoumingSettingService service;

  @GetMapping
  public Mono<RoumingSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<RoumingSettingResponse> createOrUpdate(@RequestBody RoumingSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
