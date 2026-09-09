package uz.hesap.service.log.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.Activity;
import uz.hesap.service.common.util.message.ActivityLog;
import uz.hesap.service.log.domain.ActivityLogEntity;
import uz.hesap.service.log.model.ActivityLogResponse;
import uz.hesap.service.log.model.mapper.LogMapper;
import uz.hesap.service.log.repository.ActivityLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class ActivityLogService {
  private final ActivityLogRepository activityLogRepository;
  private final LogMapper logMapper = LogMapper.INSTANCE;

  // JMS (ActivityLog) xabaridan yangi yozuv saqlaydi.
  public Mono<Void> setLog(ActivityLog reply) {
    ActivityLogEntity entity = new ActivityLogEntity();
    entity.setActorIn(reply.actorIn());
    entity.setCompanyIn(reply.companyIn());
    entity.setActivity(reply.activity());
    entity.setData(reply.data());

    return activityLogRepository
        .save(entity)
        .doOnSuccess(saved -> log.info("Activity log saved: {}", reply.activity()))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error saving activity log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }

  // Filtrlangan (actorIn/companyIn/activity/sana) sahifalangan ro'yxat.
  public Mono<Page<ActivityLogResponse>> getActivityLogs(
      String actorIn,
      String companyIn,
      Activity activity,
      Instant startDate,
      Instant endDate,
      Pageable pageable) {
    String activityName = activity != null ? activity.name() : null;

    var logsFlux =
        activityLogRepository
            .findByFilter(actorIn, companyIn, activityName, startDate, endDate, pageable)
            .map(logMapper::toResponse);

    var countMono =
        activityLogRepository.countByFilter(actorIn, companyIn, activityName, startDate, endDate);

    return Mono.zip(logsFlux.collectList(), countMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
