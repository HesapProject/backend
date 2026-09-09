package uz.hesap.service.document.service.c2c;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.ContractProductEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.repository.ContractProductRepository;
import uz.hesap.service.document.repository.DocumentRepository;
import uz.hesap.service.document.repository.PaymentScheduleRepository;
import uz.hesap.service.document.repository.ProductRequestRepository;

/**
 * Shartnomani avtomatik yakunlash (COMPLETED). Shartnoma FAQAT quyidagi ikkala shart bajarilganda
 * yopiladi:
 *
 * <ul>
 *   <li>barcha to'lov jadvali qatorlari to'liq to'langan (PAID yoki paidAmount ≥ totalAmount), va
 *   <li>barcha mahsulotlar to'liq topshirilgan (oldi-berdi) — APPROVED product_requests
 *       yig'indisi mahsulot talabini qoplasa (GOODS→quantity, MONEY→amount).
 * </ul>
 *
 * <p>Faqat ACTIVE shartnomaga ta'sir qiladi. Har to'lov/topshirish hodisasidan keyin chaqiriladi;
 * qayta-qayta chaqirilsa ham xavfsiz (idempotent — shart bajarilmasa hech narsa qilmaydi). Xatolik
 * asosiy oqimni to'xtatmasligi uchun onErrorResume bilan yutiladi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractCompletionService {

  private final DocumentRepository documentRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final ContractProductRepository contractProductRepository;
  private final ProductRequestRepository productRequestRepository;

  public Mono<Void> completeIfDone(final UUID contractId) {
    if (contractId == null) {
      return Mono.empty();
    }
    return documentRepository
        .findByIdAndDeletedFalse(contractId)
        .filter(doc -> doc.getStatus() == DocumentStatus.ACTIVE)
        .flatMap(
            doc ->
                Mono.zip(allPaid(contractId), allDelivered(contractId))
                    .flatMap(
                        t -> {
                          if (Boolean.TRUE.equals(t.getT1()) && Boolean.TRUE.equals(t.getT2())) {
                            doc.setStatus(DocumentStatus.COMPLETED);
                            log.info("Contract {} auto-completed (paid + delivered)", contractId);
                            return documentRepository.save(doc).then();
                          }
                          return Mono.empty();
                        }))
        .then()
        .onErrorResume(
            e -> {
              log.warn("completeIfDone failed for {}: {}", contractId, e.toString());
              return Mono.empty();
            });
  }

  // Barcha to'lov qatorlari to'langanmi. To'lov jadvali yo'q bo'lsa — shart bajarilgan deb qaraladi.
  private Mono<Boolean> allPaid(final UUID contractId) {
    return paymentScheduleRepository
        .findAllByContractIdAndDeletedFalse(contractId)
        .collectList()
        .map(
            list -> {
              if (list.isEmpty()) {
                return true;
              }
              return list.stream().allMatch(ContractCompletionService::isPaymentPaid);
            });
  }

  private static boolean isPaymentPaid(final PaymentEntity p) {
    if (p.getStatus() == PaymentScheduleStatus.PAID) {
      return true;
    }
    double total = p.getTotalAmount() != null ? p.getTotalAmount() : 0.0;
    double paid = p.getPaidAmount() != null ? p.getPaidAmount() : 0.0;
    return total > 0 && paid >= total;
  }

  // Barcha mahsulotlar to'liq topshirilganmi. Mahsulot yo'q bo'lsa — shart bajarilgan deb qaraladi.
  private Mono<Boolean> allDelivered(final UUID contractId) {
    return contractProductRepository
        .findAllByDocumentIdAndDeletedFalse(contractId)
        .collectList()
        .zipWith(
            productRequestRepository
                .findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(contractId)
                .filter(r -> r.getStatus() == ProductRequestStatus.APPROVED)
                .collectList())
        .map(t -> allProductsDelivered(t.getT1(), t.getT2()));
  }

  private static boolean allProductsDelivered(
      final List<ContractProductEntity> products, final List<ProductRequestEntity> approved) {
    if (products.isEmpty()) {
      return true;
    }
    for (ContractProductEntity prod : products) {
      double givenQty = 0.0;
      double givenAmt = 0.0;
      for (ProductRequestEntity r : approved) {
        if (prod.getId() != null && prod.getId().equals(r.getProductId())) {
          givenQty += r.getQuantity() != null ? r.getQuantity() : 0.0;
          givenAmt += r.getAmount() != null ? r.getAmount() : 0.0;
        }
      }
      double targetQty = prod.getQuantity() != null ? prod.getQuantity() : 0.0;
      double targetAmt = prod.getAmount() != null ? prod.getAmount() : 0.0;
      // GOODS → soni bo'yicha, MONEY → summa bo'yicha (so'rovlar faqat bitta o'qni to'ldiradi).
      boolean delivered =
          (targetQty > 0 && givenQty >= targetQty) || (targetAmt > 0 && givenAmt >= targetAmt);
      if (!delivered) {
        return false;
      }
    }
    return true;
  }
}
