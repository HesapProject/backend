package uz.hesap.service.file.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.file.model.CdnUploadResponse;
import uz.hesap.service.file.service.CdnService;

/**
 * Internal: boshqa servislardan raw byte[] upload uchun. Authsiz — gateway ushlamaydi (security
 * config'da permitAll).
 *
 * <p>Ishlatilishi: document service tomonidan generatsiya qilingan PDF'ni S3'ga saqlash.
 */
@Log4j2
@RestController
@RequestMapping("/files/v1/local/cdn")
@RequiredArgsConstructor
public class InternalCdnController {

  private final CdnService cdnService;

  @PostMapping(value = "/upload-bytes")
  public Mono<CdnUploadResponse> uploadBytes(
      @RequestParam String folderType,
      @RequestParam String filename,
      @RequestParam(required = false) UUID userId,
      ServerWebExchange exchange) {
    Flux<DataBuffer> body = exchange.getRequest().getBody();
    return DataBufferUtils.join(body)
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty body")))
        .flatMap(
            buffer -> {
              byte[] bytes = new byte[buffer.readableByteCount()];
              buffer.read(bytes);
              DataBufferUtils.release(buffer);
              return cdnService.uploadBytes(bytes, filename, folderType, userId);
            });
  }
}
