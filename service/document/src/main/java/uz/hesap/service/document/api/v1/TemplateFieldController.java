package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.model.request.TemplateFieldRequest;
import uz.hesap.service.document.model.response.TemplateFieldResponse;
import uz.hesap.service.document.service.template.TemplateFieldService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/templates/field")
public class TemplateFieldController {

  private final TemplateFieldService templateFieldService;

  @GetMapping("/{templateId}")
  public Flux<TemplateFieldResponse> getAll(@PathVariable UUID templateId) {
    return templateFieldService.getAll(templateId);
  }

  @PostMapping("/{templateId}")
  public Flux<TemplateFieldResponse> upsert(
      @PathVariable UUID templateId, @RequestBody List<TemplateFieldRequest> request) {
    return templateFieldService.upsert(templateId, request);
  }

  @DeleteMapping("/{id}")
  public Mono<TemplateFieldResponse> delete(@PathVariable UUID id) {
    return templateFieldService.delete(id);
  }
}
