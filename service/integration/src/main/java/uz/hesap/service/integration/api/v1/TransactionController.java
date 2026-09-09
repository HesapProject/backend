package uz.hesap.service.integration.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.integration.domain.TransactionEntity;
import uz.hesap.service.integration.model.TransactionResponse;
import uz.hesap.service.integration.repository.TransactionRepository;

// Balans tranzaksiyalari (ledger) — joriy foydalanuvchiniki, eng oxirgisi tepada.
@RestController
@RequestMapping("/integration/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionRepository repository;

  @GetMapping
  public Mono<Page<TransactionResponse>> list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    UUID userId = principal.user().id();
    PageRequest pageable = PageRequest.of(page, size);
    var content =
        repository
            .findAllByUserIdOrderByTimestampDesc(userId, pageable)
            .map(this::toResponse)
            .collectList();
    var count = repository.countByUserId(userId);
    return content.zipWith(count).map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  private TransactionResponse toResponse(TransactionEntity e) {
    return new TransactionResponse(
        e.getId(), e.getAmount(), e.getType(), e.getBillingType(), e.getDescription(), e.getTimestamp());
  }
}
