package uz.hesap.service.integration.service.payment;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.integration.domain.PaymentEntity;
import uz.hesap.service.integration.repository.PaymentRepository;

// To'lov muvaffaqiyatli bo'lganda "user".payments jurnaliga yozadi.
// Xato bo'lsa asosiy to'lov oqimini buzmaydi (balans allaqachon kreditlangan) — onErrorResume.
@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentRecorder {

  private final PaymentRepository paymentRepository;

  public Mono<Void> record(UUID userId, Double amount, PaymentMethod method) {
    PaymentEntity entity = new PaymentEntity();
    entity.setUserId(userId);
    entity.setAmount(amount);
    entity.setPaymentMethod(method);
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return paymentRepository
        .save(entity)
        .then()
        .onErrorResume(
            e -> {
              log.error("Failed to record payment ({}) for {}: {}", method, userId, e.getMessage());
              return Mono.empty();
            });
  }
}
