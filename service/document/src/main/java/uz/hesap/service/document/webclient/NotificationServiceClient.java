package uz.hesap.service.document.webclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.SmsReply;
import uz.hesap.service.jms.JmsPublisher;

// SMS endi RabbitMQ orqali yuboriladi: SmsReply -> integration consumer -> Eskiz.
// Avval integration'ga to'g'ridan-to'g'ri HTTP edi; fire-and-forget bo'lgani uchun
// asosiy oqimni bloklamasligi va durability/retry uchun Rabbit'ga ko'chirildi.
@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceClient {

  private final JmsPublisher jmsPublisher;

  public Mono<Void> sendSms(String phoneNumber, String message) {
    log.info("Publishing SMS to RabbitMQ for phone: {}", phoneNumber);
    return jmsPublisher.publish(new SmsReply(phoneNumber, message));
  }
}
