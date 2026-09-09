package uz.hesap.service.integration.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.TemplateNotificationRequest;
import uz.hesap.service.integration.model.TemplateNotificationResponse;
import uz.hesap.service.integration.service.TemplateNotificationService;

/** Per-template, per-event notification config (Control boshqaradi). */
@RestController
@RequestMapping("/integration/v1/templates/{templateId}/notifications")
@RequiredArgsConstructor
public class TemplateNotificationController {

  private final TemplateNotificationService templateNotificationService;

  @GetMapping
  public Mono<List<TemplateNotificationResponse>> getNotifications(@PathVariable UUID templateId) {
    return templateNotificationService.getAll(templateId);
  }

  @PutMapping
  public Mono<List<TemplateNotificationResponse>> updateNotifications(
      @PathVariable UUID templateId, @RequestBody List<TemplateNotificationRequest> requests) {
    return templateNotificationService.upsert(templateId, requests);
  }
}
