package uz.hesap.service.log.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.log.domain.PaymentReminderLogEntity;
import uz.hesap.service.log.model.PaymentReminderLogResponse;
import uz.hesap.service.log.repository.PaymentReminderLogRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class PaymentReminderLogService {

  private final PaymentReminderLogRepository repository;

  public Mono<Page<PaymentReminderLogResponse>> getLogs(final int page, final int size) {
    Pageable pageable = PageRequest.of(page, size);
    return repository
        .findAllByOrderByStartedAtDesc(pageable)
        .map(this::toResponse)
        .collectList()
        .zipWith(repository.count())
        .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  // Cron start — yangi audit yozuv, sentSuccess/Failed 0'dan boshlanadi.
  public Mono<PaymentReminderLogEntity> start() {
    PaymentReminderLogEntity e = new PaymentReminderLogEntity();
    e.setStartedAt(Instant.now());
    e.setSentSuccess(0);
    e.setSentFailed(0);
    return repository.save(e);
  }

  // Cron finish — completedAt + totalCandidates va yakuniy counter'lar.
  public Mono<Void> finish(
      java.util.UUID id, int totalCandidates, int sentSuccess, int sentFailed, String errorMessage) {
    return repository
        .findById(id)
        .flatMap(
            e -> {
              e.setCompletedAt(Instant.now());
              e.setTotalCandidates(totalCandidates);
              e.setSentSuccess(sentSuccess);
              e.setSentFailed(sentFailed);
              e.setErrorMessage(errorMessage);
              return repository.save(e);
            })
        .then();
  }

  private PaymentReminderLogResponse toResponse(PaymentReminderLogEntity e) {
    return new PaymentReminderLogResponse(
        e.getId(),
        e.getStartedAt(),
        e.getCompletedAt(),
        e.getTotalCandidates(),
        e.getSentSuccess(),
        e.getSentFailed(),
        e.getErrorMessage());
  }
}
