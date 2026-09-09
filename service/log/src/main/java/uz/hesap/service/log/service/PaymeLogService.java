package uz.hesap.service.log.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.PaymeLogReply;
import uz.hesap.service.log.domain.PaymeLogEntity;
import uz.hesap.service.log.model.PaymeLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.PaymeLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymeLogService {
  private final PaymeLogRepository paymeLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(PaymeLogReply reply) {
    PaymeLogEntity entity = new PaymeLogEntity();
    entity.setUserId(reply.userId());
    entity.setCompanyId(reply.companyId());
    entity.setType(reply.type());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return paymeLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Payme log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving Payme log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<PaymeLogResponse>> getPaymeLogs(
      UUID userId, UUID companyId, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching Payme logs with filter: userId={}, companyId={}, startDate={}, endDate={}, pageable={}",
        userId,
        companyId,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        paymeLogRepository
            .findByFilter(userId, companyId, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = paymeLogRepository.countByFilter(userId, companyId, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
