package uz.hesap.service.integration.api.v1;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.SendInviteRequest;
import uz.hesap.service.integration.model.SendInviteResponse;
import uz.hesap.service.integration.model.SmsInviteTemplateRequest;
import uz.hesap.service.integration.model.SmsInviteTemplateResponse;
import uz.hesap.service.integration.service.SmsInviteService;

@RestController
@RequestMapping("/integration/v1/eskiz")
@RequiredArgsConstructor
public class SmsInviteController {

  private final SmsInviteService smsInviteService;

  // Per-til taklif SMS shablonlari.
  @GetMapping("/template")
  public Flux<SmsInviteTemplateResponse> getTemplates() {
    return smsInviteService.getTemplates();
  }

  @PostMapping("/template")
  public Flux<SmsInviteTemplateResponse> saveTemplates(
      @RequestBody List<SmsInviteTemplateRequest> requests) {
    return smsInviteService.saveTemplates(requests);
  }

  // Fuqaroni SMS orqali taklif qilish (telefon + til).
  @PostMapping("/invite")
  public Mono<SendInviteResponse> sendInvite(@RequestBody SendInviteRequest request) {
    return smsInviteService.sendInvite(request);
  }
}
