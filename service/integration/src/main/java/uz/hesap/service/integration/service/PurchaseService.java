package uz.hesap.service.integration.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.integration.domain.PurchaseEntity;
import uz.hesap.service.integration.domain.enums.PurchaseType;
import uz.hesap.service.integration.model.PurchaseRequest;
import uz.hesap.service.integration.model.PurchaseResponse;
import uz.hesap.service.integration.repository.PurchaseRepository;

// Xaridlar jurnali CRUD + avto-yozish (tarif/paket sotib olinganda record()).
@Log4j2
@Service
@RequiredArgsConstructor
public class PurchaseService {

  private final PurchaseRepository repository;

  public Mono<Page<PurchaseResponse>> list(String userIn, Pageable pageable) {
    boolean filter = userIn != null && !userIn.isBlank();
    var contentMono =
        (filter
                ? repository.findAllByUserInOrderByCreatedAtDesc(
                    userIn, pageable.getPageSize(), pageable.getOffset())
                : repository.findAllByOrderByCreatedAtDesc(pageable))
            .map(this::toResponse)
            .collectList();
    var countMono = filter ? repository.countByUserIn(userIn) : repository.count();
    return contentMono.zipWith(countMono).map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  // Statistika: sana oralig'idagi tushum (tashqi to'lov usullari yig'indisi).
  public Mono<Double> revenue(java.time.Instant from, java.time.Instant to) {
    return repository.sumRevenueBetween(from, to);
  }

  // Admin Xaridlar: server-side sahifalash + filtrlar (paket/promo/sana).
  public Mono<Page<PurchaseResponse>> listFiltered(
      UUID unitId,
      String promo,
      java.time.Instant from,
      java.time.Instant to,
      Pageable pageable) {
    String unitIdStr = unitId == null ? null : unitId.toString();
    String promoLike = (promo == null || promo.isBlank()) ? null : "%" + promo.trim() + "%";
    var content =
        repository
            .findFiltered(
                unitIdStr, promoLike, from, to, pageable.getPageSize(), pageable.getOffset())
            .map(this::toResponse)
            .collectList();
    var count = repository.countFiltered(unitIdStr, promoLike, from, to);
    return content.zipWith(count).map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  public Mono<PurchaseResponse> getById(UUID id) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Purchase not found")))
        .map(this::toResponse);
  }

  public Mono<PurchaseResponse> create(UUID actorId, PurchaseRequest req) {
    PurchaseEntity e = new PurchaseEntity();
    e.setAmount(req.amount());
    e.setUserIn(req.userIn());
    e.setPromo(req.promo());
    e.setPaymentMethod(req.paymentMethod());
    e.setUnitType(req.unitType());
    e.setUnitId(req.unitId());
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);
    return repository.save(e).map(this::toResponse);
  }

  public Mono<PurchaseResponse> update(UUID actorId, UUID id, PurchaseRequest req) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Purchase not found")))
        .flatMap(
            e -> {
              if (req.amount() != null) e.setAmount(req.amount());
              if (req.userIn() != null) e.setUserIn(req.userIn());
              if (req.promo() != null) e.setPromo(req.promo());
              if (req.paymentMethod() != null) e.setPaymentMethod(req.paymentMethod());
              if (req.unitType() != null) e.setUnitType(req.unitType());
              if (req.unitId() != null) e.setUnitId(req.unitId());
              e.setUpdatedBy(actorId);
              return repository.save(e);
            })
        .map(this::toResponse);
  }

  public Mono<Void> delete(UUID id) {
    return repository.deleteById(id);
  }

  // Avto-yozish: tarif/paket sotib olinganda jurnalga qator qo'shadi.
  public Mono<Void> record(
      String userIn,
      Double amount,
      String promo,
      PaymentMethod paymentMethod,
      PurchaseType unitType,
      UUID unitId,
      UUID createdBy) {
    PurchaseEntity e = new PurchaseEntity();
    e.setUserIn(userIn);
    e.setAmount(amount);
    e.setPromo(promo);
    e.setPaymentMethod(paymentMethod);
    e.setUnitType(unitType);
    e.setUnitId(unitId);
    e.setCreatedBy(createdBy);
    e.setUpdatedBy(createdBy);
    return repository.save(e).then();
  }

  private PurchaseResponse toResponse(PurchaseEntity e) {
    return new PurchaseResponse(
        e.getId(),
        e.getAmount(),
        e.getUserIn(),
        e.getPromo(),
        e.getPaymentMethod(),
        e.getUnitType(),
        e.getUnitId(),
        e.getCreatedBy(),
        e.getUpdatedBy(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
