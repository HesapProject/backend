package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.request.DocumentPreviewRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/generate")
public class DocumentGenerateController {

  private final uz.hesap.service.document.service.document.DocumentGenerateService service;

  // ServerHttpResponse'ga to'g'ridan-to'g'ri yozish — content negotiation'ni butunlay
  // chetlab o'tadi. Ilgari urinishlar (produces=ALL_VALUE, ResponseEntity<Resource>)
  // baribir 406 berardi: WebFlux byte[]/Resource writer'lari Accept: application/json
  // bilan moslashmaydi. Bu yondashuv response'ga raw bayt yozadi, hech qanday writer
  // ishlatilmaydi — Accept header butunlay e'tibordan tushadi.
  // lang: uz/ru/en — ilova joriy tilida ko'rish uchun (berilmasa uz, saqlangan hujjat
  // shablonining default tili).
  @GetMapping("/contract/{contractId}")
  public Mono<Void> generate(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID contractId,
      @RequestParam(required = false) String lang,
      ServerHttpResponse response) {
    return service
        .generate(contractId, lang)
        .flatMap(pdf -> writePdf(response, pdf, "contract.pdf"));
  }

  // Talabnoma (notice) PDF — ota-hujjat datasi + klonlangan templateJson'dan render.
  @GetMapping("/notice/{noticeId}")
  public Mono<Void> generateNotice(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID noticeId,
      ServerHttpResponse response) {
    return service
        .generateNotice(noticeId)
        .flatMap(pdf -> writePdf(response, pdf, "notice.pdf"));
  }

  // Da'vo arizasi (report) PDF — ota-hujjat datasi + klonlangan templateJson'dan render.
  @GetMapping("/report/{reportId}")
  public Mono<Void> generateReport(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID reportId,
      ServerHttpResponse response) {
    return service
        .generateReport(reportId)
        .flatMap(pdf -> writePdf(response, pdf, "report.pdf"));
  }

  @PostMapping("/contract-preview")
  public Mono<Void> preview(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody DocumentPreviewRequest request,
      ServerHttpResponse response) {
    return service
        .preview(request, userPrincipal.user().identifier())
        .flatMap(pdf -> writePdf(response, pdf, "preview.pdf"));
  }

  private static Mono<Void> writePdf(ServerHttpResponse response, byte[] pdf, String filename) {
    response.setStatusCode(HttpStatus.OK);
    HttpHeaders headers = response.getHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentLength(pdf.length);
    headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
    DataBuffer buffer = response.bufferFactory().wrap(pdf);
    return response.writeWith(Mono.just(buffer));
  }
}
