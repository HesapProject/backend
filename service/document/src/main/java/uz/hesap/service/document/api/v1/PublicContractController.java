package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.service.document.DocumentGenerateService;

// Public shartnoma PDF — contract.hesap.uz/{id} PIN sahifasi to'g'ri 4 xonali kod
// bilan shu endpoint'ni chaqiradi. Auth talab qilinmaydi (SecurityConfig:
// /document/v1/public/**). Kod mos kelmasa 403, shartnoma yo'q bo'lsa 404.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/public")
public class PublicContractController {

  private final DocumentGenerateService service;
  private final DocumentRepository documentRepository;

  // GET /document/v1/public/contract/{id}?code=1234&lang=uz|ru|en — PDF inline (kod to'g'ri bo'lsa).
  @GetMapping("/contract/{contractId}")
  public Mono<Void> contractPdf(
      @PathVariable UUID contractId,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String lang,
      ServerHttpResponse response) {
    return documentRepository
        .findByIdAndDeletedFalse(contractId)
        .switchIfEmpty(Mono.defer(() -> status(response, HttpStatus.NOT_FOUND)))
        .flatMap(
            doc -> {
              String expected = doc.getAccessCode();
              if (expected == null || code == null || !expected.equals(code.trim())) {
                return status(response, HttpStatus.FORBIDDEN);
              }
              return service.generate(contractId, lang).flatMap(pdf -> writePdf(response, pdf));
            });
  }

  // Statusni o'rnatib bo'sh javob (yordamchi — reactive'da qaytariladigan Mono).
  private static <T> Mono<T> status(ServerHttpResponse response, HttpStatus statusCode) {
    response.setStatusCode(statusCode);
    return response.setComplete().then(Mono.empty());
  }

  // Raw baytlarni yozamiz — WebFlux content negotiation (Accept) chetlab o'tiladi.
  private static Mono<Void> writePdf(ServerHttpResponse response, byte[] pdf) {
    response.setStatusCode(HttpStatus.OK);
    HttpHeaders headers = response.getHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentLength(pdf.length);
    headers.setContentDisposition(ContentDisposition.inline().filename("contract.pdf").build());
    DataBuffer buffer = response.bufferFactory().wrap(pdf);
    return response.writeWith(Mono.just(buffer));
  }
}
