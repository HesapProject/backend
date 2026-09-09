package uz.hesap.service.document.api.v1;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.response.ProductLedgerResponse;
import uz.hesap.service.document.service.c2c.ProductsService;

// Oldi-berdi: shartnomalardagi mahsulotlar ledgeri.
@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/products")
public class ProductsController {

  private final ProductsService contractProductLedgerService;

  // direction yo'q bo'lsa ikkala yo'nalish; statuses yo'q bo'lsa REJECTED/CANCELLED chiqmaydi
  @GetMapping
  public Mono<Page<ProductLedgerResponse>> getProductsLedger(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(value = "direction", required = false) Direction direction,
      @RequestParam(value = "statuses", required = false) List<DocumentStatus> statuses,
      @RequestParam(value = "startDate", required = false) Instant startDate,
      @RequestParam(value = "endDate", required = false) Instant endDate,
      @RequestParam(value = "search", required = false) String search,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
    return contractProductLedgerService.getLedger(
        userPrincipal.user().id(),
        direction,
        statuses,
        startDate,
        endDate,
        search,
        PageRequest.of(page, size));
  }
}
