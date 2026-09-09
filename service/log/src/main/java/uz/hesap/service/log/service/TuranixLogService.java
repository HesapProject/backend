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
import uz.hesap.service.common.util.message.TuranixLogReply;
import uz.hesap.service.log.domain.TuranixLogEntity;
import uz.hesap.service.log.model.TuranixLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.TuranixLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class TuranixLogService {
  private final TuranixLogRepository turanixLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(TuranixLogReply reply) {
    TuranixLogEntity entity = new TuranixLogEntity();
    entity.setUserId(reply.userId());
    entity.setMsisdn(reply.msisdn());
    entity.setPinfl(reply.pinfl());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return turanixLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Turanix log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving Turanix log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<TuranixLogResponse>> getTuranixLogs(
      UUID userId, String msisdn, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching Turanix logs: userId={}, msisdn={}, startDate={}, endDate={}, pageable={}",
        userId,
        msisdn,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        turanixLogRepository
            .findByFilter(userId, msisdn, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = turanixLogRepository.countByFilter(userId, msisdn, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
