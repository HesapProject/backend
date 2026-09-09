package uz.hesap.service.main.api.v1;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.FaqRequest;
import uz.hesap.service.main.model.FaqResponse;
import uz.hesap.service.main.service.FaqService;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/main/v1/faqs")
public class FaqController {
  private final FaqService faqService;

  @PostMapping
  public Mono<FaqResponse> createFaq(@Valid @RequestBody FaqRequest request) {
    return faqService.createFaq(request);
  }

  @PutMapping("/{id}")
  public Mono<FaqResponse> updateFaq(
      @PathVariable UUID id, @Valid @RequestBody FaqRequest request) {
    return faqService.updateFaq(id, request);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> deleteFaq(@PathVariable UUID id) {
    return faqService.deleteFaq(id);
  }

  @GetMapping
  public Mono<List<FaqResponse>> getAllFaqs() {
    return faqService.getAllFaqs();
  }

  @GetMapping("/{id}")
  public Mono<FaqResponse> getFaqById(@PathVariable UUID id) {
    return faqService.getFaqById(id);
  }
}
