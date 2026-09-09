package uz.hesap.service.document.api.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.BusinessStatsResponse;
import uz.hesap.service.document.model.response.ProductLedgerStats;
import uz.hesap.service.document.service.stats.StatisticsService;

/** Biznes statistikasi — vendor Statistika dashboard uchun. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/statistics")
public class StatisticsController {

  private final StatisticsService service;

  // Aktiv entity (self yoki kompaniya) statistikasi. userId — aktiv entity id
  // (yo'q bo'lsa = principal). Company rejimida shartnoma agregatlariga
  // created_by = principal filtri qo'llanadi (DocumentController bilan bir xil).
  @GetMapping("/business")
  public Mono<BusinessStatsResponse> getBusinessStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    // userId — to'lov flow stats uchun; identifier (in/STIR) — shartnoma stats uchun.
    return service.getBusinessStats(
        userPrincipal.user().id(), userPrincipal.user().identifier());
  }

  // Oldi-berdi mahsulotlari statistikasi: kiruvchi/chiquvchi soni va summasi
  // (filterlar ProductsController ledger bilan bir xil). Avval /products/stats edi.
  @GetMapping("/products")
  public Mono<ProductLedgerStats> getProductsStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(value = "statuses", required = false) List<DocumentStatus> statuses,
      @RequestParam(value = "startDate", required = false) Instant startDate,
      @RequestParam(value = "endDate", required = false) Instant endDate,
      @RequestParam(value = "search", required = false) String search) {
    return service.getProductStats(
        userPrincipal.user().id(), statuses, startDate, endDate, search);
  }
}
