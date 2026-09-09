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
import uz.hesap.service.common.util.message.PlumLogReply;
import uz.hesap.service.log.domain.PlumLogEntity;
import uz.hesap.service.log.model.PlumLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.PlumLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class PlumLogService {
  private final PlumLogRepository plumLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(PlumLogReply reply) {
    PlumLogEntity entity = new PlumLogEntity();
    entity.setUserId(reply.userId());
    entity.setCardId(reply.cardId());
    entity.setType(reply.type());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return plumLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Plum log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving Plum log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<PlumLogResponse>> getPlumLogs(
      UUID userId, UUID cardId, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching Plum logs with filter: userId={}, cardId={}, startDate={}, endDate={}, pageable={}",
        userId,
        cardId,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        plumLogRepository
            .findByFilter(userId, cardId, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = plumLogRepository.countByFilter(userId, cardId, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
