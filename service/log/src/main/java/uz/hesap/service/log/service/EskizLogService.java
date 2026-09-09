package uz.hesap.service.log.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.EskizLogReply;
import uz.hesap.service.log.model.EskizLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.EskizLogRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class EskizLogService {

  private final EskizLogRepository eskizLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  public Mono<Void> saveLog(EskizLogReply reply) {
    log.debug("Saving Eskiz log: {}", reply);
    var entity = logMapper.toEntity(reply);
    return eskizLogRepository.save(entity).then();
  }

  public Mono<Page<EskizLogResponse>> getLogs(
      Boolean isFailed, Instant fromDate, Instant toDate, Pageable pageable) {
    log.debug("Fetching Eskiz logs with filter");
    // Eskiz log larda eng oxirgi xabar eng birinchida — timestamp DESC majburiy.
    // Client'dan kelgan Pageable'da sort bo'lmasligi mumkin, shuning uchun
    // shu yerda PageRequest bilan DESC qo'shamiz.
    Pageable sorted =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "timestamp"));
    return eskizLogRepository
        .findByFilter(isFailed, fromDate, toDate, sorted)
        .map(logMapper::toResponse)
        .collectList()
        .zipWith(eskizLogRepository.countByFilter(isFailed, fromDate, toDate))
        .map(tuple -> new PageImpl<>(tuple.getT1(), sorted, tuple.getT2()));
  }
}
