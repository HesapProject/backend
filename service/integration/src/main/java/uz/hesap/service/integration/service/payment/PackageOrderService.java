package uz.hesap.service.integration.service.payment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PaymentOrderEntity;
import uz.hesap.service.integration.repository.PaymentOrderRepository;
import uz.hesap.service.integration.webclient.MainBillingClient;

// To'g'ridan-to'g'ri paket xaridi (balanssiz) uchun kutilayotgan-order oqimi.
//  - create(): PENDING order yaratadi (checkout linkini controller alohida quradi —
//    Payme/ClickService bilan aylanma bog'liqlik bo'lmasin uchun bu yerda emas).
//  - settleIfPending(): webhook to'lovni yakunlaganda mos PENDING orderni topib
//    paketni grant qiladi (main s2s) va PAID belgilaydi. Best-effort — xato
//    bo'lsa yutiladi, to'lov webhook javobini buzmaydi (pul balansda qoladi,
//    keyin qo'lda solishtiriladi).
@Service
@RequiredArgsConstructor
@Log4j2
public class PackageOrderService {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_PAID = "PAID";

  private final PaymentOrderRepository repository;
  private final MainBillingClient mainBillingClient;

  // Checkout: PENDING order saqlaydi (amount so'mda — link va webhook so'mda).
  public Mono<PaymentOrderEntity> create(
      UUID uniqueId, String userIn, UUID packageId, Integer amount, String promoCode, String provider) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setUniqueId(uniqueId);
    order.setUserIn(userIn);
    order.setPackageId(packageId);
    order.setAmount(amount == null ? 0.0 : amount.doubleValue());
    order.setPromoCode(promoCode);
    order.setProvider(provider == null ? "PAYME" : provider.toUpperCase());
    order.setStatus(STATUS_PENDING);
    return repository.save(order);
  }

  // Webhook success'dan chaqiriladi: shu to'lovchida mos PENDING order bo'lsa —
  // PAID qilib, main'da paketni grant qiladi. Aks holda hech narsa qilmaydi.
  public Mono<Void> settleIfPending(UUID uniqueId, double amountSom) {
    return repository
        .findFirstByUniqueIdAndStatusOrderByCreatedDateDesc(uniqueId, STATUS_PENDING)
        .filter(order -> Math.round(order.getAmount()) == Math.round(amountSom))
        .flatMap(
            order -> {
              order.setStatus(STATUS_PAID);
              return repository
                  .save(order)
                  .flatMap(
                      saved ->
                          // Provider (PAYME/CLICK) — pul tashqaridan kelgan, ichki
                          // balansdan YECHILMASIN. "BALANCE" bersak withdrawBalance
                          // ishga tushib (balans 0 → xato) grant ham, xarid yozuvi
                          // ham bo'lmay qolardi.
                          mainBillingClient.grantPackage(
                              saved.getUserIn(),
                              saved.getUniqueId(),
                              saved.getPackageId(),
                              saved.getPromoCode(),
                              saved.getProvider()));
            })
        .doOnError(e -> log.error("Package order settle failed for {}: {}", uniqueId, e.getMessage()))
        .onErrorResume(e -> Mono.empty())
        .then();
  }
}
