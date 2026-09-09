package uz.hesap.service.integration.service.payment;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.BalanceEntity;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.repository.BalanceRepository;

// Payme/Click to'lovi muvaffaqiyatli bo'lganda foydalanuvchi balansini topadi/yaratadi.
// billing.BalanceService dagi minimal mantiq nusxasi — billing schema'dagi `balance` jadvali.
@Log4j2
@Service
@RequiredArgsConstructor
public class BalanceHelper {
  private final BalanceRepository balanceRepository;

  // "Muddatsiz" balans uchun uzoq-kelajak sana. Instant.MAX (yil 1e9) ISHLATILMASIN —
  // Postgres'ga 'infinity' bo'lib yoziladi, keyin R2DBC qayta serialize qilganda
  // "Invalid value for Year ... 1000000000" xatosi beradi va PerformTransaction yiqiladi.
  private static final Instant NEVER_EXPIRES = Instant.parse("9999-12-31T23:59:59Z");

  // balans entity olish (agar yo'q bo'lsa B2B sifatida yaratadi)
  public Mono<BalanceEntity> getBalanceEntity(final UUID uniqueId, final BalanceType balanceType) {
    return balanceRepository
        .findByUniqueIdAndBalanceTypeAndDeletedFalse(uniqueId, balanceType)
        .switchIfEmpty(Mono.defer(() -> createBalance(uniqueId, balanceType, BillingType.B2B)));
  }

  public Mono<BalanceEntity> getBalanceEntity(
      final UUID id, final BalanceType balanceType, final BillingType billingType) {
    return balanceRepository
        .findByUniqueIdAndBalanceTypeAndBillingTypeAndDeletedFalse(id, balanceType, billingType)
        .switchIfEmpty(Mono.defer(() -> createBalance(id, balanceType, billingType)));
  }

  private Mono<BalanceEntity> createBalance(
      UUID uniqueId, BalanceType type, BillingType billingType) {
    BalanceEntity entity = new BalanceEntity();
    entity.setBalance(0.0);
    entity.setBalanceType(type);
    entity.setUniqueId(uniqueId);
    entity.setBillingType(billingType);
    entity.setExpireDate(NEVER_EXPIRES);
    return balanceRepository.save(entity);
  }
}
