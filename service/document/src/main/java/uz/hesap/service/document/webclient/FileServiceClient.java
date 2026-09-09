package uz.hesap.service.document.webclient;

import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.context.WebClientConfig;
import uz.hesap.service.document.model.response.CdnUploadResponse;

/** File service'ga PDF cache upload va download. */
@Log4j2
@Service
public class FileServiceClient {

  private final WebClient webClient;

  @Autowired
  public FileServiceClient(
      final WebClient.Builder webClientBuilder,
      @Value("${application.file-service.base-url}") final String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  /** PDF byte[]'ni CDN'ga upload qiladi, public URL qaytaradi. */
  public Mono<String> uploadPdfBytes(byte[] bytes, String filename) {
    return webClient
        .post()
        .uri(
            b ->
                b.path("/files/v1/local/cdn/upload-bytes")
                    .queryParam("folderType", "contracts")
                    .queryParam("filename", filename)
                    .build())
        .contentType(MediaType.APPLICATION_PDF)
        .bodyValue(bytes)
        .retrieve()
        .bodyToMono(CdnUploadResponse.class)
        .map(CdnUploadResponse::contentUrl)
        .timeout(Duration.ofSeconds(30))
        .doOnError(e -> log.error("PDF upload to file service failed: {}", e.getMessage()));
  }

  /** Rasm/video byte[]'ni CDN'ga upload qiladi (folderType: images/videos), public URL qaytaradi. */
  public Mono<String> uploadMediaBytes(
      byte[] bytes, String filename, String contentType, String folderType) {
    return webClient
        .post()
        .uri(
            b ->
                b.path("/files/v1/local/cdn/upload-bytes")
                    .queryParam("folderType", folderType)
                    .queryParam("filename", filename)
                    .build())
        .contentType(MediaType.parseMediaType(contentType))
        .bodyValue(bytes)
        .retrieve()
        .bodyToMono(CdnUploadResponse.class)
        .map(CdnUploadResponse::contentUrl)
        .timeout(Duration.ofSeconds(60))
        .doOnError(e -> log.error("Media upload to file service failed: {}", e.getMessage()));
  }

  /** Saqlangan PDF'ni public URL'dan o'qib qaytaradi (S3 yoki nginx). */
  public Mono<byte[]> downloadPdfBytes(String url) {
    return WebClient.create()
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(byte[].class)
        .timeout(Duration.ofSeconds(30))
        .doOnError(e -> log.error("PDF download from {} failed: {}", url, e.getMessage()));
  }
}
