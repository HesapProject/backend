package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.request.MyIdSignRequest;
import uz.hesap.service.document.model.request.OtpRequest;

/**
 * Shartnomani guvoh (witness) sifatida imzolash oqimi: SMS yoki MyID (yuz).
 *
 * <p>Taraf (party) imzolash oqimi alohida SignController'da (/api/document/v1/sign).
 *
 * <ul>
 *   <li>SMS: sms/send -> sms/verify
 *   <li>MyID: myId
 *   <li>rad etish: reject
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/witness")
public class WitnessController {

  private final uz.hesap.service.document.service.c2c.WitnessService witnessService;

  // ---------- SMS ----------

  /** Guvohga imzolash uchun SMS OTP yuboradi. */
  @PostMapping("/sms/send/{docId}")
  public Mono<Void> sendSms(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID docId) {
    return witnessService.sendSms(docId, userPrincipal.user());
  }

  /** SMS OTP'ni tasdiqlab guvoh sifatida imzolaydi. */
  @PostMapping("/sms/verify/{docId}")
  public Mono<Void> verifySms(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody OtpRequest otpRequest) {
    return witnessService.acceptWitness(docId, userPrincipal.user().id(), otpRequest.code());
  }

  // ---------- MyID (yuz) ----------

  /** MyID (yuz) orqali guvoh sifatida imzolaydi. */
  @PostMapping("/myId/{docId}")
  public Mono<Void> signMyId(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody MyIdSignRequest request) {
    return witnessService.signWitnessByMyId(
        docId, userPrincipal.user().id(), request.code(), request.platform());
  }

  /** AbleID orqali guvoh sifatida imzolaydi. */
  @PostMapping("/able-id/{docId}")
  public Mono<Void> signAbleId(
      @PathVariable UUID docId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody uz.hesap.service.document.model.request.AbleIdSignRequest request) {
    return witnessService.signWitnessByAbleId(
        docId, userPrincipal.user().id(), request.attemptId());
  }

  // ---------- Tasdiqsiz ("oddiy") ----------

  /** Template verification turi NONE/null bo'lganda — tasdiqsiz guvoh qabul. */
  @PostMapping("/accept/{docId}")
  public Mono<Void> acceptSimple(
      @PathVariable UUID docId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return witnessService.acceptWitnessSimple(docId, userPrincipal.user().id());
  }

  // ---------- Rad etish ----------

  /** Guvoh shartnomani rad etadi. */
  @PostMapping("/reject/{docId}")
  public Mono<Void> reject(
      @PathVariable UUID docId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return witnessService.rejectWitness(docId, userPrincipal.user().id());
  }
}
