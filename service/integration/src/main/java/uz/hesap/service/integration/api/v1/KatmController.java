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
import uz.hesap.service.integration.model.katm.CreditHistoryRequest;
import uz.hesap.service.integration.model.katm.CreditHistoryResponse;
import uz.hesap.service.integration.service.katm.KatmService;

// KATM kredit byurosi — kredit tarixi so'rovi va hisoboti.
@RestController
@RequestMapping("/integration/v1/katm")
@RequiredArgsConstructor
public class KatmController {

  private final KatmService katmService;

  // Kredit tarixini so'rashni boshlaydi (init-client → submit-request).
  // Hisobot keyinroq scheduler orqali tayyor bo'ladi — status REQUESTED qaytadi.
  @PostMapping("/credit-history")
  public Mono<CreditHistoryResponse> requestCreditHistory(
      @RequestBody CreditHistoryRequest request) {
    return katmService.requestCreditHistory(request);
  }

  // Bitta so'rov/hisobotni id bo'yicha olish (reportBase64 bilan).
  @GetMapping("/credit-history/{id}")
  public Mono<CreditHistoryResponse> getById(@PathVariable UUID id) {
    return katmService.getById(id);
  }

  // Tarix: userId yoki pinfl bo'yicha, yoki barchasi.
  @GetMapping("/credit-history")
  public Flux<CreditHistoryResponse> getHistory(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String pinfl,
      Pageable pageable) {
    return katmService.getHistory(userId, pinfl, pageable);
  }
}
