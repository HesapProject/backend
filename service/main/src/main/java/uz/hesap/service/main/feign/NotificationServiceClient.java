package uz.hesap.service.main.feign;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.SmsReply;
import uz.hesap.service.jms.JmsPublisher;

/**
 * SMS yuborish endi RabbitMQ orqali: SmsReply -> integration consumer -> Eskiz. Avval integration
 * servisga to'g'ridan-to'g'ri HTTP edi; fire-and-forget bo'lgani uchun (asosiy oqimni bloklamasligi,
 * durability/retry) Rabbit'ga ko'chirildi.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationServiceClient {

  private final JmsPublisher jmsPublisher;

  public Mono<Void> sendSms(final String phone, final String message) {
    log.info("Publishing SMS to RabbitMQ for phone: {}", phone);
    return jmsPublisher.publish(new SmsReply(phone, message));
  }
}
