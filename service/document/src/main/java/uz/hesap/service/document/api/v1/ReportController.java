package uz.hesap.service.document.api.v1;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.repository.CustomDocumentRepository.ContractTemplateReport;
import uz.hesap.service.document.service.document.ReportsService;

// Analitik hisobotlar — har bir tur alohida sub-path. Hammasi shablon (turi)
// kesimida soni + summa qaytaradi. from/to — ISO-8601 instant (UTC).
//
// Skoping JWT'dan kelib chiqadi (query param emas):
//   ADMIN/SUPER_ADMIN → hammasini ko'radi (Control); ixtiyoriy `in`/`userId` bilan
//     bitta tarafga filtrlashi mumkin.
//   COMPANY           → faqat o'zinikini (identifier() = STIR) — business.
//   CLIENT            → faqat o'zinikini (identifier() = PINFL) — mijoz ilovasi.
// Admin bo'lmaganlar uchun har qanday `in`/`userId` param e'tiborga olinmaydi.
//
//   /contract            — shartnomalar (contracts.price)
//   /payment             — to'lov jadvali (payments.total_amount)
//   /payment-transaction — amalga oshgan to'lovlar (payment_transactions.amount)
//   /payment-request     — to'lov so'rovlari (payment_requests.amount)
//   /payment-delay       — kechiktirish so'rovlari (delay_requests.amount)
//   /product             — mahsulot/oldi-berdi (contract_product.amount)
//   /witness             — guvohlik (contract_witness, soni)
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/reports")
public class ReportController {

  private final ReportsService service;

  // Shartnomalar — mode=CREATED → tuzilganlar (created_date); aks holda holat
  // (ACTIVE/COMPLETED/REJECTED/CANCELLED, last_modified_date).
  @GetMapping("/contract")
  public Flux<ContractTemplateReport> contractReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false, defaultValue = "CREATED") String mode,
      @RequestParam(required = false) String in) {
    DocumentStatus status =
        "CREATED".equalsIgnoreCase(mode) ? null : DocumentStatus.valueOf(mode.toUpperCase());
    return service.contractReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to), status);
  }

  @GetMapping("/payment")
  public Flux<ContractTemplateReport> paymentReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String in) {
    return service.paymentReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to), status(status));
  }

  @GetMapping("/payment-transaction")
  public Flux<ContractTemplateReport> paymentTransactionReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String in) {
    return service.paymentTransactionReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to), status(status));
  }

  @GetMapping("/payment-request")
  public Flux<ContractTemplateReport> paymentRequestReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String in) {
    return service.paymentRequestReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to), status(status));
  }

  @GetMapping("/payment-delay")
  public Flux<ContractTemplateReport> paymentDelayReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String in) {
    return service.paymentDelayReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to), status(status));
  }

  // Mahsulot/oldi-berdi — contract_product (status ustuni yo'q, status param qabul qilinmaydi).
  @GetMapping("/product")
  public Flux<ContractTemplateReport> productReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String in) {
    return service.productReport(
        scopeIn(principal, in), Instant.parse(from), Instant.parse(to));
  }

  // Guvohlik — foydalanuvchi guvoh bo'lgan shartnomalar (witness_id, UUID).
  // status — DocumentWitnessStatus (PENDING/ACCEPTED/REJECTED).
  @GetMapping("/witness")
  public Flux<ContractTemplateReport> witnessReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID userId) {
    return service.witnessReport(
        scopeWitness(principal, userId),
        Instant.parse(from),
        Instant.parse(to),
        witnessStatus(status));
  }

  // ================ JWT skoping ================

  private static boolean isAdmin(UserPrincipal principal) {
    UserType type = principal.user().type();
    return type == UserType.ADMIN || type == UserType.SUPER_ADMIN;
  }

  // IN-asosli skop: admin → `in` override yoki null (hammasi); aks holda o'z identifikatori
  // (CLIENT=PINFL, COMPANY=STIR). Admin bo'lmasa `in` param e'tiborga olinmaydi.
  private static String scopeIn(UserPrincipal principal, String in) {
    if (isAdmin(principal)) {
      return (in != null && !in.isBlank()) ? in : null;
    }
    return principal.user().identifier();
  }

  // Guvohlik skop (witness_id, UUID): admin → `userId` override yoki null (hammasi);
  // aks holda joriy foydalanuvchi.
  private static UUID scopeWitness(UserPrincipal principal, UUID userId) {
    if (isAdmin(principal)) {
      return userId;
    }
    return principal.user().id();
  }

  // status null/bo'sh → filtr yo'q; aks holda PaymentScheduleStatus bilan tekshiriladi.
  private static String status(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return PaymentScheduleStatus.valueOf(status.toUpperCase()).name();
  }

  // Guvohlik statusi — DocumentWitnessStatus bilan tekshiriladi (null/bo'sh → filtr yo'q).
  private static String witnessStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return DocumentWitnessStatus.valueOf(status.toUpperCase()).name();
  }
}
