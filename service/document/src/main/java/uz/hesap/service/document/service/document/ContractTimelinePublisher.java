package uz.hesap.service.document.service.document;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import uz.hesap.service.common.util.message.ContractTimelineLog;
import uz.hesap.service.jms.JmsPublisher;

/**
 * Shartnoma timeline hodisalarini RabbitMQ orqali log servisga yuboradi. Har bir amalda (yaratish,
 * imzo, guvoh, to'lov/kechiktirish so'rovi, to'lov qabul) chaqiriladi. Xatolik asosiy oqimni
 * to'xtatmaydi (best-effort, onErrorResume).
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ContractTimelinePublisher {

  private final JmsPublisher jmsPublisher;

  public void publish(
      UUID contractId,
      String eventType,
      String role,
      String actorIn,
      Double amount,
      String currency) {
    publish(contractId, eventType, role, actorIn, amount, currency, null);
  }

  /**
   * Sessiya/qurilma ma'lumoti bilan (audit): meta principal'dan olinadi — kim, qaysi
   * qurilmadan va qaysi sessiyada amal qilgani Log tab'da ko'rinadi.
   */
  public void publish(
      UUID contractId,
      String eventType,
      String role,
      String actorIn,
      Double amount,
      String currency,
      TimelineMeta meta) {
    ContractTimelineLog event =
        new ContractTimelineLog(
            contractId,
            eventType,
            role,
            actorIn,
            amount,
            currency,
            Instant.now(),
            meta == null ? null : meta.sessionId(),
            meta == null ? null : meta.device());
    jmsPublisher
        .publish(event)
        .onErrorResume(
            e -> {
              log.warn("ContractTimelineLog publish failed for {}: {}", contractId, e.getMessage());
              return reactor.core.publisher.Mono.empty();
            })
        .subscribe();
  }

  public void publish(UUID contractId, String eventType, String role, String actorIn) {
    publish(contractId, eventType, role, actorIn, null, null, null);
  }

  public void publish(
      UUID contractId, String eventType, String role, String actorIn, TimelineMeta meta) {
    publish(contractId, eventType, role, actorIn, null, null, meta);
  }

  /** Sessiya id + inson o'qiy oladigan qurilma matni. */
  public record TimelineMeta(UUID sessionId, String device) {}

  /**
   * Joriy so'rov principal'idan sessiya/qurilma metasini oladi. Reaktiv zanjir ICHIDA
   * chaqirilishi shart (security context Reactor context'da yashaydi). Principal yo'q
   * bo'lsa bo'sh meta qaytadi.
   */
  public static reactor.core.publisher.Mono<TimelineMeta> currentMeta() {
    return org.springframework.security.core.context.ReactiveSecurityContextHolder.getContext()
        .map(ctx -> ctx.getAuthentication().getPrincipal())
        .filter(p -> p instanceof uz.hesap.service.common.util.UserPrincipal)
        .map(p -> (uz.hesap.service.common.util.UserPrincipal) p)
        .map(ContractTimelinePublisher::toMeta)
        .defaultIfEmpty(new TimelineMeta(null, null))
        .onErrorReturn(new TimelineMeta(null, null));
  }

  private static TimelineMeta toMeta(uz.hesap.service.common.util.UserPrincipal principal) {
    var device = principal.user() == null ? null : principal.user().device();
    if (device == null) {
      return new TimelineMeta(principal.sessionId(), null);
    }
    // "Redmi Note 8 Pro · Android 11" ko'rinishida (bo'sh qismlar tashlanadi).
    String text =
        java.util.stream.Stream.of(
                device.brand(), device.model(), device.os(), device.osVersion())
            .filter(v -> v != null && !v.isBlank())
            .distinct()
            .reduce((a, b) -> a + " " + b)
            .orElse(null);
    return new TimelineMeta(principal.sessionId(), text);
  }
}
