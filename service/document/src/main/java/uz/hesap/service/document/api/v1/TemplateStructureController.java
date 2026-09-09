package uz.hesap.service.document.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.model.response.CompileCheckResponse;
import uz.hesap.service.document.model.response.TemplateStructureResponse;
import uz.hesap.service.document.service.template.TemplateStructureService;

/** Blok-konstruktor API — struktura saqlash/o'qish, kompilyatsiya tekshiruvi. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/templates")
public class TemplateStructureController {

  private final TemplateStructureService service;

  // Saqlangan struktura (parse qilingan JSON) — yo'q bo'lsa bo'sh javob.
  @GetMapping("/{id}/structure")
  public Mono<Object> getStructure(@PathVariable UUID id) {
    return service.getStructure(id);
  }

  // Strukturani saqlaydi → JRXML generatsiya + field sync.
  @PutMapping("/{id}/structure")
  public Mono<TemplateStructureResponse> saveStructure(
      @PathVariable UUID id, @RequestBody JsonNode body) {
    return service.saveStructure(id, body.toString());
  }

  // Strukturadan JRXML yasab kompilyatsiya tekshiruvi (saqlamasdan).
  @PostMapping("/{id}/structure/compile-check")
  public Mono<CompileCheckResponse> compileCheck(
      @PathVariable UUID id, @RequestBody JsonNode body) {
    return service.compileCheck(body.toString());
  }
}
