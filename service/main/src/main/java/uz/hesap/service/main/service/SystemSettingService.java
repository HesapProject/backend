package uz.hesap.service.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.SystemSettingEntity;
import uz.hesap.service.main.repository.SystemSettingRepository;

// Tizim sozlamalari (key-value). Hozircha: Turanix telefon tekshiruvi yoniqmi.
@Service
@RequiredArgsConstructor
public class SystemSettingService {

  // Sozlama kalitlari
  public static final String TURANIX_PHONE_CHECK = "turanix.phone-check-enabled";
  public static final String SCORING_PRICE_HESAP = "scoring.price.hesap";
  public static final String SCORING_PRICE_KATM = "scoring.price.katm";
  public static final String SCORING_PRICE_PAYMENT = "scoring.price.payment";
  // Foydalanuvchi shaxsini tasdiqlash provayderi: MY_ID | ABLE_ID
  public static final String IDENTITY_PROVIDER = "identity.provider";

  // Skoring narxlari (so'm) — admin o'zgartirmaguncha default qiymatlar.
  private static final long DEFAULT_SCORING_HESAP = 10_000L;
  private static final long DEFAULT_SCORING_KATM = 20_000L;
  private static final long DEFAULT_SCORING_PAYMENT = 25_000L;

  public record ScoringPrices(long hesap, long katm, long payment) {}

  private final SystemSettingRepository repository;

  /** Turanix telefon tekshiruvi yoniqmi (default false). */
  public Mono<Boolean> isTuranixPhoneCheckEnabled() {
    return getBoolean(TURANIX_PHONE_CHECK);
  }

  /** Turanix telefon tekshiruvini yoqish/o'chirish. */
  public Mono<Void> setTuranixPhoneCheckEnabled(final boolean enabled) {
    return setValue(TURANIX_PHONE_CHECK, Boolean.toString(enabled));
  }

  /** Skoring narxlari (so'm). Sozlanmagan kalitlar uchun default qaytariladi. */
  public Mono<ScoringPrices> getScoringPrices() {
    return Mono.zip(
            getLong(SCORING_PRICE_HESAP, DEFAULT_SCORING_HESAP),
            getLong(SCORING_PRICE_KATM, DEFAULT_SCORING_KATM),
            getLong(SCORING_PRICE_PAYMENT, DEFAULT_SCORING_PAYMENT))
        .map(t -> new ScoringPrices(t.getT1(), t.getT2(), t.getT3()));
  }

  /** Skoring narxlarini saqlash (admin). */
  public Mono<Void> setScoringPrices(final long hesap, final long katm, final long payment) {
    return setValue(SCORING_PRICE_HESAP, Long.toString(hesap))
        .then(setValue(SCORING_PRICE_KATM, Long.toString(katm)))
        .then(setValue(SCORING_PRICE_PAYMENT, Long.toString(payment)));
  }

  /** Shaxs tasdiqlash provayderi (default MY_ID). */
  public Mono<String> getIdentityProvider() {
    return repository
        .findById(IDENTITY_PROVIDER)
        .map(e -> "ABLE_ID".equalsIgnoreCase(e.getSettingValue()) ? "ABLE_ID" : "MY_ID")
        .defaultIfEmpty("MY_ID");
  }

  /** Shaxs tasdiqlash provayderini o'rnatish (MY_ID | ABLE_ID). */
  public Mono<Void> setIdentityProvider(final String provider) {
    String normalized = "ABLE_ID".equalsIgnoreCase(provider) ? "ABLE_ID" : "MY_ID";
    return setValue(IDENTITY_PROVIDER, normalized);
  }

  // ===== umumiy yordamchilar =====

  private Mono<Boolean> getBoolean(final String key) {
    return repository
        .findById(key)
        .map(e -> "true".equalsIgnoreCase(e.getSettingValue()))
        .defaultIfEmpty(false);
  }

  private Mono<Long> getLong(final String key, final long fallback) {
    return repository
        .findById(key)
        .map(
            e -> {
              try {
                return Long.parseLong(e.getSettingValue());
              } catch (NumberFormatException ex) {
                return fallback;
              }
            })
        .defaultIfEmpty(fallback);
  }

  private Mono<Void> setValue(final String key, final String value) {
    return repository
        .findById(key)
        .flatMap(
            existing -> {
              existing.setSettingValue(value);
              existing.setNewRow(false);
              return repository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  SystemSettingEntity entity = new SystemSettingEntity();
                  entity.setSettingKey(key);
                  entity.setSettingValue(value);
                  entity.setNewRow(true);
                  return repository.save(entity);
                }))
        .then();
  }
}
