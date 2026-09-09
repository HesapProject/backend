package uz.hesap.service.document.service.document;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.repository.CustomDocumentRepository;
import uz.hesap.service.document.repository.CustomDocumentRepository.ContractTemplateReport;
import uz.hesap.service.document.util.Constants;

/** ReportController doirasi — shablon kesimidagi analitik hisobotlar (tur bo'yicha). */
@Service
@RequiredArgsConstructor
public class ReportsService {

  private final CustomDocumentRepository repository;

  // Shartnomalar — contracts jadvali, buyer_in/seller_in (PINFL/STIR) bo'yicha.
  // in == null → admin (hammasi).
  public Flux<ContractTemplateReport> contractReport(
      String in, Instant from, Instant to, DocumentStatus status) {
    return repository.reportByTemplate(in, from, to, status);
  }

  // To'lov jadvali (payments) — total_amount, created_at. Tomon manbada (x.buyer_in).
  public Flux<ContractTemplateReport> paymentReport(
      String userIn, Instant from, Instant to, String status) {
    return repository.reportPartyByTemplate(
        Constants.TABLE_PAYMENT_SCHEDULE, "contract_id", "total_amount", "created_at",
        "x", userIn, from, to, status);
  }

  // Amalga oshgan to'lovlar (payment_transactions) — amount, created_date.
  public Flux<ContractTemplateReport> paymentTransactionReport(
      String userIn, Instant from, Instant to, String status) {
    return repository.reportPartyByTemplate(
        Constants.TABLE_PAID_SCHEDULE, "document_id", "amount", "created_date",
        "x", userIn, from, to, status);
  }

  // To'lov so'rovlari (payment_requests) — amount, created_date.
  public Flux<ContractTemplateReport> paymentRequestReport(
      String userIn, Instant from, Instant to, String status) {
    return repository.reportPartyByTemplate(
        Constants.TABLE_PAYMENT_SCHEDULE_REQUEST, "contract_id", "amount", "created_date",
        "x", userIn, from, to, status);
  }

  // Kechiktirish so'rovlari (delay_requests) — amount, created_date.
  public Flux<ContractTemplateReport> paymentDelayReport(
      String userIn, Instant from, Instant to, String status) {
    return repository.reportPartyByTemplate(
        Constants.TABLE_DELAY_REQUEST, "contract_id", "amount", "created_date",
        "x", userIn, from, to, status);
  }

  // Mahsulot/oldi-berdi (contract_product) — amount, created_date. Manbada buyer_in
  // yo'q → tomon ota-shartnomadan (c.buyer_in). status ustuni yo'q → filtr yo'q.
  public Flux<ContractTemplateReport> productReport(
      String userIn, Instant from, Instant to) {
    return repository.reportPartyByTemplate(
        Constants.TABLE_CONTRACT_PRODUCT, "document_id", "amount", "created_date",
        "c", userIn, from, to, null);
  }

  // Guvohlik (contract_witness) — joriy foydalanuvchi guvoh bo'lgan shartnomalar soni.
  public Flux<ContractTemplateReport> witnessReport(
      UUID witnessId, Instant from, Instant to, String status) {
    return repository.reportWitnessByTemplate(witnessId, from, to, status);
  }
}
