package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.document.service.template.TemplateStructureService;
import uz.hesap.service.document.service.template.TemplatesService;

// Servis-ichi (auth'siz, /local/** permitAll): main PackageService paketlarni
// boyitishda shablon nomlarini ID'lar bo'yicha oladi
// (DocumentServiceClient.getTemplateNamesMap → POST /document/v1/local/templates).
// Eski LocalTemplateApplicationController refaktorda o'chgan edi, lekin main hali
// shu endpointga tayangani uchun /main/v1/packages 500 berardi — qayta tiklandi.
@RestController
@RequestMapping("/document/v1/local")
@RequiredArgsConstructor
public class LocalTemplatesController {

  private final TemplatesService templatesService;
  private final TemplateStructureService templateStructureService;

  @PostMapping("/templates")
  public Flux<TemplateBasicResponse> getTemplateNames(@RequestBody List<UUID> ids) {
    return templatesService.getTemplateNames(ids);
  }

  // Barcha strukturaga ega shablonlarning JRXML'sini qayta quradi (JrxmlBuilder
  // o'zgargach — masalan party manzili). Servis-ichi (auth'siz) — bir martalik
  // ishlatish uchun. Qaytadi: yangilangan shablonlar soni.
  @PostMapping("/templates/rebuild-jrxml")
  public Mono<Integer> rebuildJrxml() {
    return templateStructureService.rebuildAllJrxml();
  }

  // Diagnostika: har bir shablon bo'yicha rebuild natijasi (ok/skip/xato).
  @PostMapping("/templates/rebuild-jrxml/detailed")
  public Mono<java.util.Map<String, String>> rebuildJrxmlDetailed() {
    return templateStructureService.rebuildAllJrxmlDetailed();
  }
}
