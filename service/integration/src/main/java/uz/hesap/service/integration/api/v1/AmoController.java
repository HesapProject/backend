package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.AmoLogResponse;
import uz.hesap.service.integration.model.amo.AmoContactCommand;
import uz.hesap.service.integration.model.amo.AmoContactResponse;
import uz.hesap.service.integration.model.amo.AmoInitRequest;
import uz.hesap.service.integration.model.amo.AmoTokenView;
import uz.hesap.service.integration.service.amo.AmoAuthService;
import uz.hesap.service.integration.service.amo.AmoContactService;

// amoCRM integratsiyasi — OAuth token seeding/yangilash + qo'lda kontakt qo'shish.
// Yangi mijoz registratsiyasida kontakt avtomatik UserRegisteredEvent (RabbitMQ)
// orqali yaratiladi; bu controller esa ulanish/token va qo'lda/test uchun.
@Log4j2
@RestController
@RequestMapping("/integration/v1/amo")
@RequiredArgsConstructor
public class AmoController {

  private final AmoAuthService authService;
  private final AmoContactService contactService;

  // Boshlang'ich OAuth ulanish (authorization_code) — bir martalik seeding.
  @PostMapping("/auth/init")
  public Mono<AmoTokenView> initAuth(@RequestBody AmoInitRequest request) {
    return authService.initAuth(request);
  }

  // Tokenni refresh_token bilan qo'lda yangilash.
  @PostMapping("/auth/update")
  public Mono<AmoTokenView> updateToken() {
    return authService.updateToken();
  }

  // Joriy token holati (maxfiy qiymatlarsiz).
  @GetMapping("/auth/token")
  public Mono<AmoTokenView> token() {
    return authService.tokenView();
  }

  // Qo'lda/test kontakt qo'shish.
  @PostMapping("/contacts/add")
  public Mono<AmoContactResponse> addContact(@RequestBody AmoContactCommand command) {
    return contactService.addContact(command);
  }

  // Monitoring loglari (kontakt yaratish tarixi) — paged.
  @GetMapping("/logs")
  public Flux<AmoLogResponse> logs(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return contactService.logs(page, size);
  }
}
