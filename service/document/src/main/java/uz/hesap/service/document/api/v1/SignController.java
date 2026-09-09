package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.SignDocumentRequest;
import uz.hesap.service.document.model.request.AbleIdSignRequest;
import uz.hesap.service.document.model.request.MyIdSignRequest;
import uz.hesap.service.document.model.request.OtpRequest;

/**
 * Shartnoma tarafini (buyer/seller) imzolash oqimi: SMS yoki MyID (yuz).
 *
 * <p>Guvoh (witness) oqimi alohida WitnessController'da (/api/document/v1/witness).
 *
 * <ul>
 *   <li>SMS: sms/send -> sms/verify
 *   <li>MyID: myId
 *   <li>rad etish: reject
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/sign")
public class SignController {

  private final uz.hesap.service.document.service.c2c.SignService signService;

  // ---------- E-IMZO (ContractController'dan ko'chirildi) ----------

  /** E-IMZO PKCS7 uchun timestamp oladi. */
  @PostMapping("/e-imzo/timestamp")
  public Mono<String> getEimzoTimestamp(
      @RequestBody SignDocumentRequest requestBody, ServerHttpRequest request) {
    return signService.getTimestamp(requestBody.pkcs7(), request);
  }

  /** E-IMZO PKCS7 bilan shartnomani imzolaydi. */
  @PostMapping("/e-imzo")
  public Mono<Void> signEimzo(
      @RequestBody SignDocumentRequest requestBody, ServerHttpRequest request) {
    return signService.signDocument(requestBody.pkcs7(), request);
  }

  // ---------- SMS ----------

  /** Tarafga imzolash uchun SMS OTP yuboradi. */
  @PostMapping("/sms/send/{docId}")
  public Mono<Void> sendSms(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID docId) {
    return signService.sendSms(docId, userPrincipal.user());
  }

  /** SMS OTP'ni tasdiqlab taraf sifatida imzolaydi. */
  @PostMapping("/sms/verify/{docId}")
  public Mono<Void> verifySms(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody OtpRequest otpRequest) {
    return signService.acceptParty(
        docId, userPrincipal.user().id(), otpRequest.code(), otpRequest.userPackageId());
  }

  // ---------- Tasdiqsiz (template NONE) ----------

  /** Template tasdiqlash turi NONE bo'lsa taraf OTP/MyID'siz imzolaydi. */
  @PostMapping("/accept/{docId}")
  public Mono<Void> acceptSimple(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody(required = false) SimpleAcceptRequest request) {
    return signService.acceptPartySimple(
        docId, userPrincipal.user().id(), request == null ? null : request.userPackageId());
  }

  public record SimpleAcceptRequest(UUID userPackageId) {}

  // ---------- MyID (yuz) ----------

  /** MyID (yuz) orqali taraf sifatida imzolaydi. */
  @PostMapping("/myId/{docId}")
  public Mono<Void> signMyId(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody MyIdSignRequest request) {
    return signService.signPartyByMyId(
        docId, userPrincipal.user().id(), request.code(), request.platform(),
        request.userPackageId());
  }

  // ---------- AbleID (yuz, liveness) ----------

  /** AbleID orqali taraf sifatida imzolaydi. */
  @PostMapping("/able-id/{docId}")
  public Mono<Void> signAbleId(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody AbleIdSignRequest request) {
    return signService.signPartyByAbleId(
        docId, userPrincipal.user().id(), request.attemptId(), request.userPackageId());
  }

  // ---------- Rad etish ----------

  /** Taraf shartnomani rad etadi (yaratuvchi bo'lsa bekor qiladi). */
  @PostMapping("/reject/{docId}")
  public Mono<Void> reject(
      @PathVariable UUID docId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return signService.rejectParty(docId, userPrincipal.user().id());
  }

  // Faol shartnomani bekor qilish endi cancel_requests orqali (CancelRequestsController).
}
