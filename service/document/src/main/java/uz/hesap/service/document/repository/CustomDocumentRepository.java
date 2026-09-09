package uz.hesap.service.document.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.PendingDocumentStats;

public interface CustomDocumentRepository {

  // Berilgan kun uchun keyingi ketma-ket raqam (atomik, concurrency-safe).
  // Shartnoma raqami YYMMDD-NNNN uchun NNNN qismi.
  Mono<Integer> nextDailyNumber(LocalDate date);

  // Preview uchun: keyingi raqamni INCREMENT QILMASDAN qaytaradi (peek).
  // Hali saqlanmagan hujjatda taxminiy raqamni ko'rsatish uchun.
  Mono<Integer> peekDailyNumber(LocalDate date);

  // pending documentlar — taraf identifikatori (in/STIR) bo'yicha count.
  Mono<PendingDocumentStats> getPendingStats(String in);

  // universal filter. `in` — taraf identifikatori (buyer_in/seller_in, self uchun), optional.
  // from/to — created_date bo'yicha sana oralig'i (ixtiyoriy, admin ro'yxati uchun).
  Flux<DocumentEntity> findFiltered(
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to,
      Pageable pageable);

  Mono<Long> countFiltered(
      String in,
      List<DocumentStatus> statuses,
      UUID templateId,
      String search,
      Instant from,
      Instant to);

  // Ikki taraf orasidagi shartnomalar (o'zaro) — buyer_in/seller_in juftligi {inA, inB}.
  Flux<DocumentEntity> findBetweenParties(String inA, String inB, Pageable pageable);

  Mono<Long> countBetweenParties(String inA, String inB);

  // Taraf (in) shartnomalari soni status bo'yicha — filter chiplar uchun.
  Flux<DocumentStatusCount> countByStatus(String in);

  record DocumentStatusCount(DocumentStatus status, long count) {}

  // ACTIVE shartnomalar currency kesimida: soni + umumiy qiymati (price yig'indisi).
  Flux<BusinessStatsResponse.ContractCurrencyStats> getActiveStatsByCurrency(String in);

  // Yaratishda buyer_in/seller_in ni user jadvalidan (COALESCE(pinfl,tin)) to'ldiradi.
  // user/document bir DB'da — cross-schema UPDATE.
  Mono<Void> populatePartyIns(UUID docId, UUID buyerUserId, UUID sellerUserId);

  // Hisobot: [from,to) oralig'idagi shartnomalar shablon (turi) kesimida soni + summa.
  // in — taraf buyer_in/seller_in (PINFL/STIR); null/bo'sh → filtr yo'q (admin hammasi).
  // status == null → tuzilganlar (created_date); status != null → o'sha holatdagilar
  // (ACTIVE/COMPLETED/REJECTED/CANCELLED, last_modified_date bo'yicha).
  Flux<ContractTemplateReport> reportByTemplate(
      String in, Instant from, Instant to, DocumentStatus status);

  // IN-asosli hisoboti: berilgan manba jadvali (payments / payment_transactions /
  // payment_requests / delay_requests / contract_product) kesimida, shartnoma shabloni
  // bo'yicha guruhlangan soni + summa. scopeAlias — buyer_in/seller_in qaysi alias'da
  // ("x" manba yoki "c" contracts). userIn null/bo'sh → taraf filtri yo'q (admin hammasi).
  // status null → barchasi. sourceTable/contractFkCol/amountCol/dateCol/scopeAlias —
  // controllerdan keladigan ishonchli konstantalar (foydalanuvchi kiritmaydi).
  Flux<ContractTemplateReport> reportPartyByTemplate(
      String sourceTable,
      String contractFkCol,
      String amountCol,
      String dateCol,
      String scopeAlias,
      String userIn,
      Instant from,
      Instant to,
      String status);

  // Guvohlik hisoboti: foydalanuvchi guvoh bo'lgan (contract_witness.witness_id)
  // shartnomalar, shablon kesimida soni (summa yo'q → total = 0).
  Flux<ContractTemplateReport> reportWitnessByTemplate(
      UUID witnessId, Instant from, Instant to, String status);

  record ContractTemplateReport(
      UUID templateId,
      String nameUz,
      String nameRu,
      String nameEn,
      long count,
      double totalAmount) {}
}
