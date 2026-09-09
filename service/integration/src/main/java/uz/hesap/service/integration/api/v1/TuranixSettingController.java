package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.TuranixSettingRequest;
import uz.hesap.service.integration.model.TuranixSettingResponse;
import uz.hesap.service.integration.service.TuranixSettingService;

@RestController
@RequestMapping("/integration/v1/turanix/settings")
@RequiredArgsConstructor
public class TuranixSettingController {

  private final TuranixSettingService service;

  @GetMapping
  public Mono<TuranixSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<TuranixSettingResponse> createOrUpdate(@RequestBody TuranixSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
