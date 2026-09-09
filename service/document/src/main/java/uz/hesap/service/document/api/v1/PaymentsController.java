package uz.hesap.service.document.api.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentFilterStatus;
import uz.hesap.service.document.model.response.B2BPaymentScheduleResponse;
import uz.hesap.service.document.service.payment.PaymentsService;

// To'lov jadvallari (payments) — filtrlangan ro'yxat.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/payments")
public class PaymentsController {

  private final PaymentsService b2bPaymentQueryService;

  // Filtrlar (hammasi ixtiyoriy):
  //  ?startDate=&endDate=  — payment_date oralig'i
  //  ?buyerIn=             — hujjat buyer_in (PINFL/STIR)
  //  ?sellerIn=            — hujjat seller_in
  //  ?contractId=          — hujjat (document) id
  //  ?statuses=            — active(overdue)/pending/done/partly (to'lov holati)
  //  ?contractStatuses=    — hujjat holati (masalan ACTIVE) — faqat shu holatdagi
  //                          shartnomalar to'lovlari (Home aktiv shartnomalar uchun)
  @GetMapping
  public Mono<Page<B2BPaymentScheduleResponse>> getPayments(
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String buyerIn,
      @RequestParam(required = false) String sellerIn,
      @RequestParam(required = false) UUID contractId,
      @RequestParam(required = false) List<PaymentFilterStatus> statuses,
      @RequestParam(required = false) List<DocumentStatus> contractStatuses,
      Pageable pageable) {
    return b2bPaymentQueryService.getPaymentsFiltered(
        startDate, endDate, buyerIn, sellerIn, contractId, statuses, contractStatuses, pageable);
  }
}
