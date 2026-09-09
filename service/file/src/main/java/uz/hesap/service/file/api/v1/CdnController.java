package uz.hesap.service.file.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.file.model.CdnUploadResponse;
import uz.hesap.service.file.service.CdnService;

@Log4j2
@RestController
@RequestMapping("/files/v1/cdn")
@RequiredArgsConstructor
public class CdnController {

  private static final long CACHE_MAX_AGE_SECONDS = 365L * 24 * 3600; // 1 year

  private final CdnService cdnService;

  @PostMapping("/upload")
  public Mono<CdnUploadResponse> upload(
      @AuthenticationPrincipal UserPrincipal user,
      @RequestParam String type,
      @RequestPart("file") Mono<FilePart> filePartMono) {

    return filePartMono
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided")))
        .flatMap(filePart -> cdnService.upload(filePart, type, user.user().id()));
  }

  /**
   * Public download — cdn.hesap.uz orqali tashqi clientlar fayllarni shu yo'lda oladi. Nginx
   * \`cdn.hesap.uz/{folder}/{filename}\` → \`file:8004/api/files/v1/cdn/{folder}/{filename}\`.
   */
  @GetMapping("/{folder}/{filename}")
  public Mono<Void> download(
      @PathVariable String folder,
      @PathVariable String filename,
      ServerHttpResponse response) {
    return cdnService
        .downloadBytes(folder, filename)
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")))
        .flatMap(
            result -> {
              response.setStatusCode(HttpStatus.OK);
              HttpHeaders headers = response.getHeaders();
              if (result.contentType() != null) {
                headers.setContentType(MediaType.parseMediaType(result.contentType()));
              } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
              }
              headers.setContentLength(result.bytes().length);
              headers.setCacheControl(
                  CacheControl.maxAge(java.time.Duration.ofSeconds(CACHE_MAX_AGE_SECONDS))
                      .cachePublic()
                      .immutable());
              DataBuffer buffer = response.bufferFactory().wrap(result.bytes());
              return response.writeWith(Mono.just(buffer));
            });
  }
}
