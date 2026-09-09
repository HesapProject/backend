package uz.hesap.service.document.service.c2c;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.document.model.enums.ActionType;
import uz.hesap.service.document.webclient.NotificationServiceClient;

@Service
@AllArgsConstructor
@Log4j2
public class VerificationService {

  private final NotificationServiceClient notificationService;

  // In-memory cache for OTP. In production with multiple instances, use Redis.
  private final Cache<String, String> otpCache =
      Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).maximumSize(10000).build();

  public Mono<Void> sendOtp(UUID userId, UUID docId, ActionType action, String phoneNumber) {
    String key = generateKey(userId, docId, action);
    // Resend: keshda OTP bo'lsa o'shani qayta yuboramiz (ilgari yuborilgan SMS
    // ham amal qilsin), yo'q bo'lsa yangi 5 xonali kod. Har holda SMS yuboriladi
    // va TTL yangilanadi — ilgari "oldKey != null -> Mono.empty()" jim o'tib
    // ketib, foydalanuvchi yangi SMS olmay qolardi.
    String existing = otpCache.getIfPresent(key);
    String otp =
        existing != null
            ? existing
            : String.valueOf(ThreadLocalRandom.current().nextInt(10000, 100000));
    otpCache.put(key, otp); // (qayta) keshlash + 3-daqiqalik TTL ni yangilash
    log.debug("OTP (re)sent for key {}", key);

    // Bu OTP shartnomani imzolash uchun (taraf/guvoh) — auth/registratsiya emas.
    String message =
        "Hesap.uz ilovasida shartnomani imzolash uchun tasdiqlash kod: " + otp;
    return notificationService.sendSms(phoneNumber, message);
  }

  // Reaktiv: throw emas Mono.error — aks holda flatMap'da 500 bo'lishi mumkin.
  public Mono<Void> verifyOtp(UUID userId, UUID docId, ActionType action, Integer code) {
    String key = generateKey(userId, docId, action);
    String cachedOtp = otpCache.getIfPresent(key);

    if (cachedOtp == null) {
      return Mono.error(new BadRequestException("OTP expired or not found"));
    }
    if (!cachedOtp.equals(code.toString())) {
      return Mono.error(new BadRequestException("Invalid OTP code"));
    }
    // Invalidate after use
    otpCache.invalidate(key);
    return Mono.empty();
  }

  private String generateKey(UUID userId, UUID docId, ActionType action) {
    return userId + "_" + docId + "_" + (action != null ? action.name() : "PARTY_ACCEPT");
  }
}
