package uz.hesap.service.log.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserLogModel;
import uz.hesap.service.common.util.Utils;
import uz.hesap.service.log.domain.UserLogEntity;
import uz.hesap.service.log.repository.UserLogRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserLogService {
  private final UserLogRepository userLogRepository;

  public Mono<Void> setLog(UserLogModel reply) {
    UserLogEntity entity = new UserLogEntity();
    entity.setUserId(reply.userId());
    entity.setReason(reply.reason());
    entity.setFirstName(reply.firstName());
    entity.setLastName(reply.lastName());
    entity.setNewVersion(reply.newVersion());
    if (reply.oldVersion() == null) {
      entity.setOldVersion(null);
      entity.setDifferentFields(null);
      return userLogRepository
          .save(entity)
          .doOnNext(System.out::println)
          .doOnSuccess(saved -> log.info("Log saved successfully"))
          .then()
          .onErrorResume(
              e -> {
                log.error("Error occurred while saving log: {}", e.getMessage(), e);
                return Mono.error(e);
              });
    }
    return Mono.fromCallable(() -> Utils.getDifferentFields(reply.oldVersion(), reply.newVersion()))
        .flatMap(
            differentFields -> {
              entity.setOldVersion(reply.oldVersion());
              entity.setDifferentFields(differentFields.isEmpty() ? null : differentFields);
              return userLogRepository.save(entity);
            })
        .doOnSuccess(saved -> log.info("Log saved successfully"))
        .then()
        .onErrorResume(
            e -> {
              log.error("Error occurred while saving log: {}", e.getMessage(), e);
              return Mono.error(e);
            });
  }
}
