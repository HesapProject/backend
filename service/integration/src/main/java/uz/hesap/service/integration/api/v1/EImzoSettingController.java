package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.EImzoSettingRequest;
import uz.hesap.service.integration.model.EImzoSettingResponse;
import uz.hesap.service.integration.service.EImzoSettingService;

@RestController
@RequestMapping("/integration/v1/eimzo/settings")
@RequiredArgsConstructor
public class EImzoSettingController {

  private final EImzoSettingService service;

  @GetMapping
  public Mono<EImzoSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<EImzoSettingResponse> createOrUpdate(@RequestBody EImzoSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
