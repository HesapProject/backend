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
import uz.hesap.service.common.util.message.MyIdLogReply;
import uz.hesap.service.log.domain.MyIdLogEntity;
import uz.hesap.service.log.model.MyIdLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.MyIdLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class MyIdLogService {
  private final MyIdLogRepository myIdLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> setLog(MyIdLogReply reply) {
    MyIdLogEntity entity = new MyIdLogEntity();
    entity.setUserId(reply.userId());
    entity.setCompanyId(reply.companyId());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return myIdLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("MyId log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving MyId log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Page<MyIdLogResponse>> getMyIdLogs(
      UUID companyId, UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
    log.debug(
        "Fetching MyId logs with filter: companyId={}, userId={}, startDate={}, endDate={}, pageable={}",
        companyId,
        userId,
        startDate,
        endDate,
        pageable);

    var logsFlux =
        myIdLogRepository
            .findByFilter(companyId, userId, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono = myIdLogRepository.countByFilter(companyId, userId, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
