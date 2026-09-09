package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.request.PaymentRequestActionRequest;
import uz.hesap.service.document.model.request.PaymentScheduleRequestRequest;
import uz.hesap.service.document.model.response.PaymentScheduleRequestResponse;
import uz.hesap.service.document.service.payment.PaymentRequestsService;

// To'lov so'rovlari (payment request): yaratish, qabul (approve), rad (reject), bekor (cancel),
// va filtrlangan ro'yxat.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/payment-requests")
public class PaymentRequestsController {

  private final PaymentRequestsService service;
  private final uz.hesap.service.document.webclient.FileServiceClient fileServiceClient;

  private static final long MAX_ATTACHMENT_BYTES = 30L * 1024 * 1024; // 30MB

  // Chek rasm/video biriktirma: raw body -> CDN -> public URL.
  // Qaytgan URL create body'dagi `image` maydoniga qo'yiladi.
  @PostMapping("/attachment")
  public Mono<java.util.Map<String, String>> uploadAttachment(
      @RequestHeader(value = "Content-Type", required = false) String contentType,
      org.springframework.web.server.ServerWebExchange exchange) {
    String ct = contentType == null ? "application/octet-stream" : contentType;
    boolean video = ct.startsWith("video/");
    String ext =
        switch (ct) {
          case "image/png" -> "png";
          case "image/webp" -> "webp";
          case "image/heic" -> "heic";
          case "video/quicktime" -> "mov";
          case "video/mp4" -> "mp4";
          default -> video ? "mp4" : "jpg";
        };
    String filename = UUID.randomUUID() + "." + ext;
    String folder = video ? "videos" : "images";
    return org.springframework.core.io.buffer.DataBufferUtils.join(
            exchange.getRequest().getBody(), (int) MAX_ATTACHMENT_BYTES)
        .switchIfEmpty(
            Mono.error(
                new uz.hesap.service.common.exception.BadRequestException("Fayl bo'sh")))
        .flatMap(
            buffer -> {
              byte[] bytes = new byte[buffer.readableByteCount()];
              buffer.read(bytes);
              org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
              return fileServiceClient.uploadMediaBytes(bytes, filename, ct, folder);
            })
        .map(url -> java.util.Map.of("url", url));
  }

  // Yaratish — body'dagi paymentScheduleId bo'yicha (documentId schedule'dan olinadi).
  @PostMapping
  public Mono<Void> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentScheduleRequestRequest request) {
    return service.create(userPrincipal.user(), request);
  }

  // Qabul qilish (approve) — id body'da.
  @PostMapping("/approve")
  public Mono<Void> approve(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PaymentRequestActionRequest request) {
    return service.approve(request.id(), userPrincipal);
  }

  // Rad etish (reject) — id body'da.
  @PostMapping("/reject")
  public Mono<Void> reject(@RequestBody PaymentRequestActionRequest request) {
    return service.reject(request.id());
  }

  // Bekor qilish (cancel) — id body'da.
  @PostMapping("/cancel")
  public Mono<Void> cancel(@RequestBody PaymentRequestActionRequest request) {
    return service.cancel(request.id());
  }

  // Filtrlangan ro'yxat:
  //  ?paymentId=   — shu paymentScheduleId ga tegishli so'rovlar
  //  ?receiverIn=  — shu IN (PINFL/STIR) ga yuborilgan so'rovlar (hujjat seller_in)
  //  ?statuses=    — tegishli statuslardagi so'rovlar (PENDING/APPROVED/CANCELLED/PAID)
  //  ?fromIn=      — men yuborgan so'rovlar (hujjat buyer_in)
  //  ?toIn=        — menga kelgan so'rovlar (hujjat seller_in)
  @GetMapping
  public Flux<PaymentScheduleRequestResponse> getRequests(
      @RequestParam(required = false) UUID paymentId,
      @RequestParam(required = false) String receiverIn,
      @RequestParam(required = false) String fromIn,
      @RequestParam(required = false) String toIn,
      @RequestParam(required = false) List<PaymentScheduleStatus> statuses) {
    return service.getRequests(paymentId, receiverIn, fromIn, toIn, statuses);
  }
}
