package uz.hesap.service.file.service;

import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import uz.hesap.service.file.context.CdnProperties;
import uz.hesap.service.file.domain.CdnDataEntity;
import uz.hesap.service.file.model.CdnUploadResponse;
import uz.hesap.service.file.repository.CdnDataRepository;
import uz.hesap.service.file.util.CdnFolderType;
import uz.hesap.service.file.util.UtilFunctions;

@Log4j2
@Service
@RequiredArgsConstructor
public class CdnService {

  private final S3AsyncClient s3Client;
  private final CdnProperties cdnProperties;
  private final CdnDataRepository cdnDataRepository;

  public Mono<CdnUploadResponse> upload(FilePart filePart, String folderType, UUID userId) {
    if (!CdnFolderType.isValidFolder(folderType)) {
      return Mono.error(new IllegalArgumentException("Invalid folder type: " + folderType));
    }

    String originalFilename = filePart.filename();
    String ext = UtilFunctions.getFileExtension(originalFilename);
    String fileName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
    String key = folderType + "/" + fileName;

    return DataBufferUtils.join(filePart.content())
        .flatMap(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);

              long contentLength = bytes.length;
              String contentUrl = cdnProperties.getPublicUrl(folderType, fileName);

              PutObjectRequest request =
                  PutObjectRequest.builder()
                      .bucket(cdnProperties.getBucket())
                      .key(key)
                      .acl(ObjectCannedACL.PUBLIC_READ)
                      .contentType(getContentType(ext))
                      .build();

              return Mono.fromFuture(
                      s3Client.putObject(
                          request, AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(bytes))))
                  .doOnSuccess(resp -> log.info("CDN upload success: {} for user {}", key, userId))
                  .doOnError(e -> log.error("CDN upload failed: {}", e.getMessage()))
                  .flatMap(
                      resp ->
                          saveUploadHistory(userId, contentLength, ext, folderType, contentUrl)
                              .thenReturn(new CdnUploadResponse(contentUrl)));
            });
  }

  /**
   * Internal: boshqa servislardan kelgan raw byte[]'ni S3'ga saqlash (masalan document service
   * generatsiya qilgan PDF). FilePart'siz, fayl nomi chaqiruvchi tomonidan beriladi (PDF cache
   * uchun deterministik nom). InternalCdnController ishlatadi.
   */
  public Mono<CdnUploadResponse> uploadBytes(
      byte[] bytes, String filename, String folderType, UUID userId) {
    if (!CdnFolderType.isValidFolder(folderType)) {
      return Mono.error(new IllegalArgumentException("Invalid folder type: " + folderType));
    }

    String ext = UtilFunctions.getFileExtension(filename);
    String key = folderType + "/" + filename;
    long contentLength = bytes.length;
    String contentUrl = cdnProperties.getPublicUrl(folderType, filename);

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(cdnProperties.getBucket())
            .key(key)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .contentType(getContentType(ext))
            .build();

    return Mono.fromFuture(
            s3Client.putObject(request, AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(bytes))))
        .doOnSuccess(resp -> log.info("CDN uploadBytes success: {} for user {}", key, userId))
        .doOnError(e -> log.error("CDN uploadBytes failed: {}", e.getMessage()))
        .flatMap(
            resp ->
                saveUploadHistory(userId, contentLength, ext, folderType, contentUrl)
                    .thenReturn(new CdnUploadResponse(contentUrl)));
  }

  /**
   * S3'dan fayl bytes va content-type olish. cdn.hesap.uz domeni orqali tashqi clientlar
   * fayllarni shu yo'l bilan oladi (nginx → file service → S3).
   */
  public Mono<DownloadResult> downloadBytes(String folder, String fileName) {
    String key = folder + "/" + fileName;
    GetObjectRequest request =
        GetObjectRequest.builder().bucket(cdnProperties.getBucket()).key(key).build();
    return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toBytes()))
        .map(
            (ResponseBytes<GetObjectResponse> rb) -> {
              GetObjectResponse resp = rb.response();
              return new DownloadResult(rb.asByteArray(), resp.contentType());
            })
        .onErrorResume(
            NoSuchKeyException.class,
            e -> {
              log.warn("S3 key not found: {}", key);
              return Mono.empty();
            });
  }

  public record DownloadResult(byte[] bytes, String contentType) {}

  private Mono<CdnDataEntity> saveUploadHistory(
      UUID userId, long contentLength, String ext, String folder, String contentUrl) {
    CdnDataEntity entity = new CdnDataEntity();
    entity.setUserId(userId);
    entity.setContentLength(contentLength);
    entity.setExt(ext);
    entity.setFolder(folder);
    entity.setContentUrl(contentUrl);
    return cdnDataRepository
        .save(entity)
        .doOnSuccess(saved -> log.debug("CDN history saved: {}", saved.getId()))
        .doOnError(e -> log.error("Failed to save CDN history: {}", e.getMessage()));
  }

  private String getContentType(String ext) {
    return switch (ext.toLowerCase()) {
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "webp" -> "image/webp";
      case "mp4" -> "video/mp4";
      case "webm" -> "video/webm";
      case "mov" -> "video/quicktime";
      default -> "application/octet-stream";
    };
  }
}
