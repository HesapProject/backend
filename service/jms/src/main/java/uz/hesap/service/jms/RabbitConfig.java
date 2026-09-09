package uz.hesap.service.jms;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

@Configuration
public class RabbitConfig {

  private static final Logger LOGGER = LogManager.getLogger();

  private final AmqpAdmin amqpAdmin;
  private final ApplicationContext applicationContext;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${spring.rabbitmq.host}")
  private String host;

  @Value("${spring.rabbitmq.port}")
  private Integer port;

  @Value("${spring.rabbitmq.username}")
  private String username;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Value("${spring.rabbitmq.exchange}")
  private String exchange;

  public RabbitConfig(final AmqpAdmin amqpAdmin, final ApplicationContext applicationContext) {
    this.amqpAdmin = amqpAdmin;
    this.applicationContext = applicationContext;
  }

  @Bean(destroyMethod = "close")
  Connection connection() throws IOException, TimeoutException {
    LOGGER.info(
        "Preparing connection on [{}:{}] RabbitMQ Server on [{}]", host, port, applicationName);

    final ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(host);
    connectionFactory.setPort(port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    return connectionFactory.newConnection(applicationName + "-rabbit");
  }

  @PostConstruct
  public void init() {
    final Exchange exchange = ExchangeBuilder.directExchange(this.exchange).durable(true).build();
    amqpAdmin.declareExchange(exchange);

    final Map<String, Object> springBoot =
        applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
    final String mainPackage =
        springBoot.isEmpty() ? null : springBoot.values().toArray()[0].getClass().getPackageName();

    LOGGER.debug("Main package: [{}]", mainPackage);

    if (mainPackage == null) {
      return;
    }

    final ClassPathScanningCandidateComponentProvider provider = createComponentScanner();
    for (final BeanDefinition beanDef : provider.findCandidateComponents(mainPackage)) {
      bindQueueToExchangeWithRoutingKey(amqpAdmin, exchange, beanDef);
    }
  }

  @Bean(destroyMethod = "close")
  Sender sender(final Connection connection) {
    return RabbitFlux.createSender(
        new SenderOptions().connectionMono(Mono.fromCallable(() -> connection).cache()));
  }

  @Bean(destroyMethod = "close")
  Receiver receiver(final Connection connection) {
    return RabbitFlux.createReceiver(
        new ReceiverOptions().connectionMono(Mono.fromCallable(() -> connection).cache()));
  }

  private ClassPathScanningCandidateComponentProvider createComponentScanner() {
    final ClassPathScanningCandidateComponentProvider provider =
        new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(JmsConsumer.class));
    return provider;
  }

  private void bindQueueToExchangeWithRoutingKey(
      final AmqpAdmin amqpAdmin, final Exchange exchange, final BeanDefinition beanDef) {
    try {
      final Class<?> clazz = Class.forName(beanDef.getBeanClassName());
      final JmsConsumer consumer = clazz.getAnnotation(JmsConsumer.class);
      final String queueName = String.format("%s.%s", applicationName, consumer.targetType());
      final Queue queue = QueueBuilder.durable(queueName).build();
      amqpAdmin.declareQueue(queue);

      final String routingKey = consumer.targetType();

      final Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs();
      amqpAdmin.declareBinding(binding);
    } catch (final Exception e) {
      LOGGER.error("Failed to bind exchange, queue and routing key", e);
    }
  }
}
