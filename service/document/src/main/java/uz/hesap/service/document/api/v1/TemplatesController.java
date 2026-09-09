package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.model.request.TemplateRequest;
import uz.hesap.service.document.model.response.TemplateResponse;
import uz.hesap.service.document.service.template.TemplatesService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/templates")
public class TemplatesController {

  private final TemplatesService templateService;

  // status — ixtiyoriy filtr (masalan PUBLISHED). Berilmasa hammasi (Control uchun).
  @GetMapping
  public Flux<TemplateResponse> getAll(
      @RequestParam(required = false) TemplateType type,
      @RequestParam(required = false) TemplateStatus status) {
    return templateService.getAll(type, status);
  }

  @GetMapping("/{id}")
  public Mono<TemplateResponse> getById(@PathVariable UUID id) {
    return templateService.getById(id);
  }

  @PostMapping
  public Mono<TemplateResponse> create(@RequestBody TemplateRequest request) {
    return templateService.create(request);
  }

  @PutMapping("/{id}")
  public Mono<TemplateResponse> edit(@PathVariable UUID id, @RequestBody TemplateRequest request) {
    return templateService.edit(id, request);
  }

  @DeleteMapping("/{id}")
  public Mono<TemplateResponse> delete(@PathVariable UUID id) {
    return templateService.delete(id);
  }
}
