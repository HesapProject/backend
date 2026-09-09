package uz.hesap.service.document.api.v1;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import uz.hesap.service.common.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.request.DocumentRequest;
import uz.hesap.service.document.model.response.CreatedDocumentResponse;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.service.document.ContractsService;

// Shartnomalar (yagona controller — eski /contract bu yerga birlashtirildi):
//  (param yo'q)          — JORIY foydalanuvchining o'z shartnomalari (identifier bo'yicha)
//  ?in=<PINFL/STIR>      — o'sha tarafning BARCHA shartnomalari
//  ?mutual=<PINFL/STIR>  — o'sha taraf va JORIY foydalanuvchi orasidagi O'ZARO shartnomalar
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/contracts")
public class ContractsController {

  private final ContractsService documentService;

  @GetMapping
  public Mono<Page<DocumentEnrichedResponse>> getContracts(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false) String in,
      @RequestParam(required = false) String mutual,
      // ?ins=<inA>,<inB> — ikki TARAF (PINFL/STIR) orasidagi o'zaro shartnomalar
      // (joriy foydalanuvchi shart emas — partnerning hamkori konteksti uchun).
      @RequestParam(required = false) List<String> ins,
      // statuses — ko'p tanlovli filter (?statuses=CREATED&statuses=COMPLETED yoki
      // ?statuses=CREATED,COMPLETED). Bo'sh/yo'q bo'lsa — barcha statuslar.
      @RequestParam(required = false) List<DocumentStatus> statuses,
      @RequestParam(required = false) UUID templateId,
      @RequestParam(required = false, defaultValue = "false") Boolean withValues,
      // Hujjat raqami / taraf bo'yicha qidiruv (admin ro'yxati uchun).
      @RequestParam(required = false) String search,
      // Sana oralig'i (created_date): ?from=2026-01-01&to=2026-01-31 (kun) yoki ISO instant.
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      Pageable pageable) {
    String me = userPrincipal.user().identifier();
    // ?ins=A,B — ikki aniq taraf orasidagi o'zaro shartnomalar.
    if (ins != null && ins.size() >= 2) {
      return documentService.getBetween(ins.get(0), ins.get(1), pageable);
    }
    // O'zaro: joriy foydalanuvchi (identifier) va `mutual` (in) orasidagi shartnomalar.
    if (mutual != null && !mutual.isBlank()) {
      return documentService.getBetween(me, mutual, pageable);
    }
    // `in` berilsa — o'sha taraf; aks holda parametrsiz = joriy foydalanuvchining o'zi.
    // Admin tokenida identifier null → target null → BARCHA hujjat (filtrlanmaydi).
    // Company token'da identifier = company STIR; user o'chsa ham buyer_in/seller_in barqaror.
    String target = (in != null && !in.isBlank()) ? in : me;
    return documentService.getAll(
        target, pageable, statuses, templateId, withValues, search, parseFrom(from), parseTo(to));
  }

  // "2026-01-31" (kun) yoki to'liq ISO instant qabul qilinadi. Kun berilsa:
  // from -> kun boshi, to -> kun oxiri (23:59:59.999) — oraliq ikki tomonlama kiruvchi.
  private static Instant parseFrom(String v) {
    return parseBoundary(v, false);
  }

  private static Instant parseTo(String v) {
    return parseBoundary(v, true);
  }

  private static Instant parseBoundary(String v, boolean endOfDay) {
    if (v == null || v.isBlank()) return null;
    String s = v.trim();
    try {
      if (s.length() == 10) {
        LocalDate d = LocalDate.parse(s);
        return endOfDay
            ? d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1)
            : d.atStartOfDay(ZoneOffset.UTC).toInstant();
      }
      return Instant.parse(s);
    } catch (Exception e) {
      throw new BadRequestException("Sana formati noto'g'ri: " + v);
    }
  }

  // Shartnomalar soni status bo'yicha — filter chiplar uchun (taraf identifikatori bo'yicha).
  @GetMapping("/status-counts")
  public Mono<Map<DocumentStatus, Long>> getStatusCounts(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    return documentService.getStatusCounts(userPrincipal.user().identifier());
  }

  @GetMapping("/{id}")
  public Mono<DocumentEnrichedResponse> getById(@PathVariable UUID id) {
    return documentService.getById(id);
  }

  // Shartnoma yaratish. C2C user (CLIENT/COMPANY OneID) — company yo'q, null o'tkaziladi.
  // Company linkidagi B2B user — uning faol company'si bilan saqlanadi.
  @PostMapping
  public Mono<CreatedDocumentResponse> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody DocumentRequest request) {
    return documentService
        .create(request, userPrincipal.user().id(), userPrincipal.user().identifier())
        .map(CreatedDocumentResponse::new);
  }

  // Shartnomani tahrirlash.
  @PutMapping("/{id}")
  public Mono<Void> edit(@PathVariable UUID id, @RequestBody DocumentRequest request) {
    return documentService.edit(id, request);
  }

  // Shartnomani o'chirish.
  @DeleteMapping("/{id}")
  public Mono<Void> delete(@PathVariable UUID id) {
    return documentService.delete(id);
  }
}
