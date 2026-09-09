package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.CardResponse;
import uz.hesap.service.integration.model.uzcard.*;
import uz.hesap.service.integration.service.scoring.PlumService;

// Karta CRUD — Plum provider. URL: /api/integration/v1/plum/card
@RestController
@RequestMapping("/integration/v1/plum/card")
@RequiredArgsConstructor
public class PlumCardController {

  private final PlumService plumService;

  @PostMapping("/create")
  public Mono<CreateUserCardResponse> createUserCard(@RequestBody CreateUserCardRequest request) {
    return plumService.createUserCard(request);
  }

  @PostMapping("/confirm")
  public Mono<ConfirmUserCardResponse> confirmUserCard(
      @RequestBody ConfirmUserCardRequest request) {
    return plumService.confirmUserCard(request);
  }

  @GetMapping("/resend-otp")
  public Mono<ResendOtpResponse> resendOtp(@RequestParam Integer session) {
    return plumService.resendOtp(session);
  }

  @DeleteMapping("/delete/{cardId}")
  public Mono<DeleteUserCardResponse> deleteUserCard(@PathVariable UUID cardId) {
    return plumService.deleteUserCard(cardId);
  }

  // Orphan tozalash: karta Plum'da bor lekin bizning DB'da yo'q ("Bu karta allaqachon
  // qo'shilgan" xatosidan keyin). Plum'dan PINFL bo'yicha topib, Plum'ning o'zidan
  // o'chiradi — shundan so'ng karta qayta qo'shilishi mumkin.
  @PostMapping("/clean-orphan")
  public Mono<DeleteUserCardResponse> cleanOrphan(@RequestBody CleanOrphanCardRequest request) {
    return plumService.cleanOrphanCard(request.pinfl(), request.cardNumber());
  }

  // Kartalar PINFL/STIR (user_in) bo'yicha.
  @GetMapping("/{userIn}")
  public Flux<CardResponse> getUserCards(@PathVariable String userIn) {
    return plumService.getUserCards(userIn);
  }

  // Kartadan pul yechib foydalanuvchi balansini to'ldiradi. Karta trusted bo'lmasa OTP so'raydi
  // (javobda otpRequired=true, session) — keyin /payment/confirm chaqiriladi.
  @PostMapping("/payment")
  public Mono<PlumPaymentResponse> payment(@RequestBody PlumPaymentRequest request) {
    return plumService.payment(request);
  }

  // OTP bilan to'lovni tasdiqlaydi (session + otp) → muvaffaqiyatda balans to'ldiriladi.
  @PostMapping("/payment/confirm")
  public Mono<PlumPaymentResponse> confirmPayment(@RequestBody PaymentConfirmRequest request) {
    return plumService.confirmPayment(request);
  }
}
