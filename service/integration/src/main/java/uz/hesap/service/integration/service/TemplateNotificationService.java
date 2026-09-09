package uz.hesap.service.integration.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.integration.domain.TemplateNotificationEntity;
import uz.hesap.service.integration.model.NotificationConfig;
import uz.hesap.service.integration.model.TemplateNotificationRequest;
import uz.hesap.service.integration.model.TemplateNotificationResponse;
import uz.hesap.service.integration.repository.TemplateNotificationRepository;

/**
 * Per-template, per-event notification config (SMS/Firebase yoq-o'chiq + matn). Config egasi —
 * integration servis. Consumer {@link #resolve} orqali yuborish qaroriga keladi; Control {@link
 * #getAll}/{@link #upsert} orqali boshqaradi.
 */
@Service
@RequiredArgsConstructor
public class TemplateNotificationService {

  private final TemplateNotificationRepository repository;

  /** 7 hodisa ro'yxati — saqlanmaganlariga default (push yoniq, sms o'chiq) to'ldiriladi. */
  public Mono<List<TemplateNotificationResponse>> getAll(UUID templateId) {
    return repository
        .findAllByTemplateIdAndDeletedFalse(templateId)
        .collectList()
        .map(
            saved -> {
              Map<NotificationEvent, TemplateNotificationEntity> byEvent =
                  new EnumMap<>(NotificationEvent.class);
              saved.forEach(e -> byEvent.put(e.getEvent(), e));
              List<TemplateNotificationResponse> result = new ArrayList<>();
              for (NotificationEvent event : NotificationEvent.values()) {
                TemplateNotificationEntity e = byEvent.get(event);
                if (e == null) {
                  NotificationConfig d = NotificationConfig.defaults();
                  result.add(
                      new TemplateNotificationResponse(
                          event, d.smsEnabled(), d.smsText(), d.firebaseEnabled(), d.firebaseText()));
                } else {
                  result.add(
                      new TemplateNotificationResponse(
                          event,
                          Boolean.TRUE.equals(e.getSmsEnabled()),
                          e.getSmsText(),
                          !Boolean.FALSE.equals(e.getFirebaseEnabled()),
                          e.getFirebaseText()));
                }
              }
              return result;
            });
  }

  /** Bir hodisa uchun yechilgan config; qator yo'q bo'lsa default. */
  public Mono<NotificationConfig> resolve(UUID templateId, NotificationEvent event) {
    return repository
        .findByTemplateIdAndEventAndDeletedFalse(templateId, event)
        .map(
            e ->
                new NotificationConfig(
                    Boolean.TRUE.equals(e.getSmsEnabled()),
                    e.getSmsText(),
                    !Boolean.FALSE.equals(e.getFirebaseEnabled()),
                    e.getFirebaseText()))
        .defaultIfEmpty(NotificationConfig.defaults());
  }

  /** Har hodisa bo'yicha insert/update; oxirida yangilangan ro'yxat. */
  public Mono<List<TemplateNotificationResponse>> upsert(
      UUID templateId, List<TemplateNotificationRequest> requests) {
    return Flux.fromIterable(requests)
        .filter(r -> r.event() != null)
        .concatMap(r -> upsertOne(templateId, r))
        .then(getAll(templateId));
  }

  private Mono<TemplateNotificationEntity> upsertOne(
      UUID templateId, TemplateNotificationRequest req) {
    return repository
        .findByTemplateIdAndEventAndDeletedFalse(templateId, req.event())
        .defaultIfEmpty(newEntity(templateId, req.event()))
        .flatMap(
            e -> {
              apply(e, req);
              return repository.save(e);
            });
  }

  private TemplateNotificationEntity newEntity(UUID templateId, NotificationEvent event) {
    TemplateNotificationEntity e = new TemplateNotificationEntity();
    e.setTemplateId(templateId);
    e.setEvent(event);
    return e;
  }

  private void apply(TemplateNotificationEntity e, TemplateNotificationRequest req) {
    if (req.smsEnabled() != null) e.setSmsEnabled(req.smsEnabled());
    if (req.firebaseEnabled() != null) e.setFirebaseEnabled(req.firebaseEnabled());
    e.setSmsText(blankToNull(req.smsText()));
    e.setFirebaseText(blankToNull(req.firebaseText()));
  }

  private String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}
