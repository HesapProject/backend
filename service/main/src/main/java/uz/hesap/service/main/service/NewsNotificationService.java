package uz.hesap.service.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.FirebaseTopicReply;
import uz.hesap.service.common.util.message.NotificationType;
import uz.hesap.service.jms.JmsPublisher;
import uz.hesap.service.main.model.SendNewsNotification;
import uz.hesap.service.main.repository.NewsRepository;

// News e'lon: broadcast in-app bildirishnoma (RabbitMQ -> integration consumer DB'ga saqlaydi,
// userId=null -> FCM yo'q) + topic FCM push (integration). FCM/in-app logikasi integration'da.
@Log4j2
@Service
@RequiredArgsConstructor
public class NewsNotificationService {

  private static final String BLOG_TOPIC = "BLOG";

  private final NewsRepository blogRepository;
  private final JmsPublisher jmsPublisher;

  public Mono<Void> publishNews(SendNewsNotification request) {
    return blogRepository
        .findById(request.blogId())
        .switchIfEmpty(Mono.error(new NotFoundException("News not found")))
        .flatMap(
            blog -> {
              TextModel title =
                  new TextModel(blog.getTitleUz(), blog.getTitleRu(), blog.getTitleEn());
              TextModel body = new TextModel(blog.getBodyUz(), blog.getBodyRu(), blog.getBodyEn());
              FirebaseNotificationReply inApp =
                  new FirebaseNotificationReply(
                      blog.getId(), title, body, NotificationType.NEWS, null, null, null);
              FirebaseTopicReply topic =
                  new FirebaseTopicReply(blog.getId(), blog.getId(), BLOG_TOPIC, title, body, null);
              // Ikkalasi ham RabbitMQ orqali: in-app (integration DB'ga saqlaydi) + topic FCM push.
              return jmsPublisher.publish(inApp).then(jmsPublisher.publish(topic));
            });
  }
}
