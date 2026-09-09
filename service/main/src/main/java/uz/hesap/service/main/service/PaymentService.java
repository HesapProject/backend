package uz.hesap.service.main.service;

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
import uz.hesap.service.main.domain.PaymentEntity;
import uz.hesap.service.main.model.PaymentRequest;
import uz.hesap.service.main.model.PaymentResponse;
import uz.hesap.service.main.repository.PaymentRepository;

// To'lovlar jurnali CRUD + avto-yozish (Control admin income va boshqalar uchun record()).
@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository repository;

  public Mono<Page<PaymentResponse>> list(UUID userId, Pageable pageable) {
    var contentMono =
        (userId != null
                ? repository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                : repository.findAllByOrderByCreatedAtDesc(pageable))
            .map(this::toResponse)
            .collectList();
    var countMono = userId != null ? repository.countByUserId(userId) : repository.count();
    return contentMono
        .zipWith(countMono)
        .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  public Mono<PaymentResponse> getById(UUID id) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment not found")))
        .map(this::toResponse);
  }

  public Mono<PaymentResponse> create(UUID actorId, PaymentRequest req) {
    PaymentEntity e = new PaymentEntity();
    e.setAmount(req.amount());
    e.setUserId(req.userId());
    e.setPaymentMethod(req.paymentMethod());
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);
    return repository.save(e).map(this::toResponse);
  }

  public Mono<PaymentResponse> update(UUID actorId, UUID id, PaymentRequest req) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Payment not found")))
        .flatMap(
            e -> {
              if (req.amount() != null) e.setAmount(req.amount());
              if (req.userId() != null) e.setUserId(req.userId());
              if (req.paymentMethod() != null) e.setPaymentMethod(req.paymentMethod());
              e.setUpdatedBy(actorId);
              return repository.save(e);
            })
        .map(this::toResponse);
  }

  public Mono<Void> delete(UUID id) {
    return repository.deleteById(id);
  }

  // Avto-yozish: to'lov amalga oshganda jurnalga qator qo'shadi. createdBy null bo'lishi mumkin.
  public Mono<Void> record(UUID userId, Double amount, PaymentMethod method, UUID createdBy) {
    PaymentEntity e = new PaymentEntity();
    e.setUserId(userId);
    e.setAmount(amount);
    e.setPaymentMethod(method);
    e.setCreatedBy(createdBy);
    e.setUpdatedBy(createdBy);
    return repository.save(e).then();
  }

  private PaymentResponse toResponse(PaymentEntity e) {
    return new PaymentResponse(
        e.getId(),
        e.getAmount(),
        e.getUserId(),
        e.getPaymentMethod(),
        e.getCreatedBy(),
        e.getUpdatedBy(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
