package uz.hesap.api.gateway.context;

import static reactor.netty.resources.ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.net.ssl.SSLException;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

  @Bean
  HttpClient httpClient() throws SSLException {
    SslContext sslContext =
        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return HttpClient.create(
            ConnectionProvider.builder("webflux")
                .evictInBackground(Duration.ofMinutes(1))
                .lifo()
                .maxConnections(500)
                .metrics(true)
                .maxIdleTime(Duration.ofMinutes(1))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofMillis(DEFAULT_POOL_ACQUIRE_TIMEOUT))
                .build())
        .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
        .responseTimeout(Duration.of(20, ChronoUnit.SECONDS));
  }

  @Bean
  WebClientCustomizer webClientCustomizer() {
    return (webClientBuilder) -> {
      try {
        webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient()));
      } catch (SSLException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
