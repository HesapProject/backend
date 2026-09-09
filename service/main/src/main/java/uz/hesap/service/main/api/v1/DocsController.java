package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.LegalDocumentType;
import uz.hesap.service.main.model.DocsRequest;
import uz.hesap.service.main.model.DocsResponse;
import uz.hesap.service.main.service.LegalDocumentService;

// About/Terms/Privacy bitta controllerda — type (enum) bilan. GET hammaga,
// PUT faqat admin/super_admin.
@RestController
@RequestMapping("/main/v1/docs")
@RequiredArgsConstructor
public class DocsController {

  private final LegalDocumentService legalDocumentService;

  // Hujjatni o'qish — ochiq.
  @GetMapping("/{type}")
  public Mono<DocsResponse> get(@PathVariable LegalDocumentType type) {
    return legalDocumentService.getDoc(type);
  }

  // Hujjatni yangilash — faqat admin/super_admin.
  @PutMapping("/{type}")
  public Mono<DocsResponse> update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable LegalDocumentType type,
      @RequestBody DocsRequest request) {
    final UserType role = principal.user().type();
    if (role != UserType.ADMIN && role != UserType.SUPER_ADMIN) {
      return Mono.error(new ForbiddenException("Faqat admin yangilay oladi"));
    }
    return legalDocumentService.saveDoc(type, request.content());
  }
}
