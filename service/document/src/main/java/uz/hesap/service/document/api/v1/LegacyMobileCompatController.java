package uz.hesap.service.document.api.v1;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentFilterStatus;
import uz.hesap.service.document.model.response.B2BPaymentScheduleResponse;
import uz.hesap.service.document.service.c2c.SignService;
import uz.hesap.service.document.service.c2c.WitnessService;
import uz.hesap.service.document.service.document.ContractsService;
import uz.hesap.service.document.service.payment.PaymentsService;

/**
 * Do'konlarda tarqalgan ESKI mobil build'lar uchun moslik yo'llari.
 *
 * <p>Android (Play Store) hali eski C2C manzillarini chaqiradi — ular yangi backendda
 * boshqa nom oldi. Ilovani yangilashsiz ishlashi uchun shu yo'llar yangi servislarga
 * bog'lanadi; bu yerda biznes-mantiq YO'Q.
 *
 * <ul>
 *   <li>contracts/{id}/send-verification-sms -> sign|witness sms/send
 *   <li>contracts/{id}/party/accept|reject -> sign/sms/verify | sign/reject
 *   <li>contracts/pending/payments -> payments (joriy foydalanuvchi, to'lanmaganlar)
 *   <li>contracts/pending-stats -> contracts/status-counts
 * </ul>
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1")
public class LegacyMobileCompatController {

  private final SignService signService;
  private final WitnessService witnessService;
  private final PaymentsService paymentsService;
  private final ContractsService documentService;

  // Eski: ?action=CREATE|ACCEPT|WITNESS. WITNESS bo'lsa guvoh OTP'si, aks holda taraf OTP'si.
  @PostMapping("/contracts/{docId}/send-verification-sms")
  public Mono<Void> sendVerificationSms(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID docId,
      @RequestParam(required = false) String action) {
    boolean witness = action != null && action.toUpperCase().contains("WITNESS");
    return witness
        ? witnessService.sendSms(docId, userPrincipal.user())
        : signService.sendSms(docId, userPrincipal.user());
  }

  // Eski: ?code=12345 (bo'sh bo'lsa — tasdiqsiz shablon uchun oddiy qabul).
  @PostMapping("/contracts/{docId}/party/accept")
  public Mono<Void> acceptParty(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID docId,
      @RequestParam(required = false) String code) {
    UUID userId = userPrincipal.user().id();
    Integer otp = parseCode(code);
    return otp == null
        ? signService.acceptPartySimple(docId, userId, null)
        : signService.acceptParty(docId, userId, otp, null);
  }

  @PostMapping("/contracts/{docId}/party/reject")
  public Mono<Void> rejectParty(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID docId) {
    return signService.rejectParty(docId, userPrincipal.user().id());
  }

  // Eski "kutilayotgan to'lovlar": joriy foydalanuvchi aktiv shartnomalaridagi
  // to'lanmagan (muddati o'tgan/kelmagan/qisman) to'lovlar. ?role=LENDER|DEBTOR.
  @GetMapping("/contracts/pending/payments")
  public Mono<Page<B2BPaymentScheduleResponse>> pendingPayments(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    String me = userPrincipal.user().identifier();
    // role=LENDER — men haqdorman (seller_in), aks holda qarzdor (buyer_in).
    // Eski ilovada default ko'rinish qarzdorniki edi — role berilmasa shu qoladi.
    boolean lender = role != null && role.toUpperCase().startsWith("LENDER");
    String buyerIn = lender ? null : me;
    String sellerIn = lender ? me : null;
    return paymentsService.getPaymentsFiltered(
        null,
        null,
        buyerIn,
        sellerIn,
        null,
        List.of(PaymentFilterStatus.ACTIVE, PaymentFilterStatus.PENDING, PaymentFilterStatus.PARTLY),
        List.of(DocumentStatus.ACTIVE),
        PageRequest.of(page, size));
  }

  // Eski nom — yangi: /document/v1/contracts/status-counts
  @GetMapping("/contracts/pending-stats")
  public Mono<Map<DocumentStatus, Long>> pendingStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return documentService.getStatusCounts(userPrincipal.user().identifier());
  }

  private static Integer parseCode(String code) {
    if (code == null || code.isBlank()) return null;
    try {
      return Integer.valueOf(code.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
