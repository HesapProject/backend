package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.PlumSettingRequest;
import uz.hesap.service.integration.model.PlumSettingResponse;
import uz.hesap.service.integration.service.PlumSettingService;

@RestController
@RequestMapping("/integration/v1/plum/settings")
@RequiredArgsConstructor
public class PlumSettingController {

  private final PlumSettingService service;

  @GetMapping
  public Mono<PlumSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<PlumSettingResponse> createOrUpdate(@RequestBody PlumSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
