package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.MyIdSettingRequest;
import uz.hesap.service.integration.model.MyIdSettingResponse;
import uz.hesap.service.integration.service.MyIdSettingService;

// Admin uchun MyID credential boshqaruvi. Response'da clientSecret yo'q.
@RestController
@RequestMapping("/integration/v1/myid/settings")
@RequiredArgsConstructor
public class MyIdSettingController {

  private final MyIdSettingService service;

  @GetMapping
  public Mono<MyIdSettingResponse> get() {
    return service.get();
  }

  @PostMapping
  public Mono<MyIdSettingResponse> createOrUpdate(@RequestBody MyIdSettingRequest request) {
    return service.createOrUpdate(request);
  }
}
