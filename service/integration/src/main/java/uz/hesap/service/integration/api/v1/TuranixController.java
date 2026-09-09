package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.turanix.TuranixCheckRequest;
import uz.hesap.service.integration.model.turanix.TuranixCheckResponse;
import uz.hesap.service.integration.service.turanix.TuranixService;

// Turanix partnyor-servis. URL'lar turanix/<feat> ko'rinishida — yangi feat
// qo'shilsa shu yerga endpoint qo'shiladi (turanix/msisdn, turanix/...).
@RestController
@RequestMapping("/integration/v1/turanix")
@RequiredArgsConstructor
public class TuranixController {

  private final TuranixService turanixService;

  // MSISDN pasport/PINFL'ga biriktirilganini tekshiradi.
  @PostMapping("/msisdn")
  public Mono<TuranixCheckResponse> checkPassMsisdn(@RequestBody TuranixCheckRequest request) {
    return turanixService.checkPassMsisdn(request);
  }

  // Bitta tekshiruvni id bo'yicha olish.
  @GetMapping("/msisdn/{id}")
  public Mono<TuranixCheckResponse> getById(@PathVariable UUID id) {
    return turanixService.getById(id);
  }

  // Tarix: userId yoki msisdn bo'yicha, yoki barchasi.
  @GetMapping("/msisdn")
  public Flux<TuranixCheckResponse> getHistory(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String msisdn,
      Pageable pageable) {
    return turanixService.getHistory(userId, msisdn, pageable);
  }
}
