package uz.hesap.service.log.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.RoumingLogReply;
import uz.hesap.service.log.domain.RoumingLogEntity;
import uz.hesap.service.log.model.RoumingLogResponse;
import uz.hesap.service.log.repository.RoumingLogRepository;

// Rouming (ЭСФ) draft yuborish loglari — RabbitMQ'dan yozish + admin ro'yxati.
@Log4j2
@Service
@RequiredArgsConstructor
public class RoumingLogService {
  private final RoumingLogRepository roumingLogRepository;

  public Mono<Void> setLog(RoumingLogReply reply) {
    RoumingLogEntity entity = new RoumingLogEntity();
    entity.setContractId(reply.contractId());
    entity.setContractNo(reply.contractNo());
    entity.setFacturaNo(reply.facturaNo());
    entity.setSellerTin(reply.sellerTin());
    entity.setBuyerTin(reply.buyerTin());
    entity.setStatus(reply.status());
    entity.setErrorMessage(reply.errorMessage());
    entity.setRequest(reply.request());
    entity.setResponse(reply.response());
    entity.setRequestTime(reply.time());

    return roumingLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Rouming log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving Rouming log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  // Sana filtri ixtiyoriy — berilmasa keng oraliq (hammasi).
  public Mono<Page<RoumingLogResponse>> getRoumingLogs(
      Instant startDate, Instant endDate, Pageable pageable) {
    Instant from = startDate != null ? startDate : Instant.EPOCH;
    Instant to = endDate != null ? endDate : Instant.now().plusSeconds(86400);
    var logsFlux =
        roumingLogRepository
            .findAllByCreatedDateBetweenOrderByCreatedDateDesc(from, to, pageable)
            .map(this::toResponse);
    var countMono = roumingLogRepository.countByCreatedDateBetween(from, to);
    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  private RoumingLogResponse toResponse(RoumingLogEntity e) {
    return new RoumingLogResponse(
        e.getId(),
        e.getContractId(),
        e.getContractNo(),
        e.getFacturaNo(),
        e.getSellerTin(),
        e.getBuyerTin(),
        e.getStatus(),
        e.getErrorMessage(),
        e.getRequest(),
        e.getResponse(),
        e.getRequestTime(),
        e.getCreatedDate());
  }
}
