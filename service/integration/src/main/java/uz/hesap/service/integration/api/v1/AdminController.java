package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.CardResponse;
import uz.hesap.service.integration.model.oneid.OneIdPassportResponse;
import uz.hesap.service.integration.repository.UserCardRepository;
import uz.hesap.service.integration.service.OneIdProfileService;
import uz.hesap.service.integration.webclient.UserServiceClient;

// Admin endpoints — Control admin paneli uchun.
@Log4j2
@RestController
@RequestMapping("/integration/v1/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UserCardRepository userCardRepository;
  private final OneIdProfileService oneIdProfileService;
  private final UserServiceClient userServiceClient;

  // Mijoz info sahifa "Kartalar" tab'i uchun — billing.plum_cards'dan o'qiydi
  // (cross-schema). Endi user_in (PINFL) bo'yicha; userId berilsa PINFL'ga resolish.
  @GetMapping("/cards")
  public Flux<CardResponse> getUserCards(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String pinfl) {
    Mono<String> resolvedIn =
        (pinfl != null && !pinfl.isBlank())
            ? Mono.just(pinfl)
            : (userId != null
                ? userServiceClient.getUserById(userId).map(u -> u.identifier())
                : Mono.empty());
    return resolvedIn
        .flatMapMany(userCardRepository::findByUserIn)
        .map(
            c ->
                new CardResponse(
                    c.getId(), c.getUserIn(), c.getCardNumber(), c.getExpireDate(), c.getType()));
  }

  // Mijoz info sahifa "Manzil" kartasi uchun — OneID profilidan (manzil,
  // tug'ilgan sana/joy, pasport va h.k.). Profil topilmasa bo'sh qaytadi.
  @GetMapping("/oneid/{userId}")
  public Mono<OneIdPassportResponse> getOneIdProfile(@PathVariable UUID userId) {
    return oneIdProfileService.getPassport(userId);
  }
}
