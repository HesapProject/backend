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
import uz.hesap.service.common.util.message.ClickLogReply;
import uz.hesap.service.log.domain.ClickLogEntity;
import uz.hesap.service.log.model.ClickLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.ClickLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClickLogService {
  private final ClickLogRepository clickLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(ClickLogReply reply) {
    ClickLogEntity entity = new ClickLogEntity();
    entity.setUserId(reply.userId());
    entity.setCompanyId(reply.companyId());
    entity.setType(reply.type());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return clickLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Click log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving Click log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<ClickLogResponse>> getClickLogs(
      UUID userId, UUID companyId, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching Click logs with filter: userId={}, companyId={}, startDate={}, endDate={}, pageable={}",
        userId,
        companyId,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        clickLogRepository
            .findByFilter(userId, companyId, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = clickLogRepository.countByFilter(userId, companyId, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
