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
import uz.hesap.service.common.util.message.OneIdLogReply;
import uz.hesap.service.log.domain.OneIdLogEntity;
import uz.hesap.service.log.model.OneIdLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.OneIdLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class OneIdLogService {
  private final OneIdLogRepository oneIdLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(OneIdLogReply reply) {
    OneIdLogEntity entity = new OneIdLogEntity();
    entity.setUserId(reply.userId());
    entity.setFirstName(reply.firstName());
    entity.setLastName(reply.lastName());
    entity.setCompanyId(reply.companyId());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequestTime(reply.time());
    return oneIdLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<OneIdLogResponse>> getOneIdLogs(
      UUID companyId, UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching OneId logs with filter: companyId={}, userId={}, startDate={}, endDate={}, pageable={}",
        companyId,
        userId,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        oneIdLogRepository
            .findByFilter(companyId, userId, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = oneIdLogRepository.countByFilter(companyId, userId, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
