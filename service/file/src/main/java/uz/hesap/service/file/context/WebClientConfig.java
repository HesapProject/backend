package uz.hesap.service.file.context;

import static reactor.netty.resources.ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

  @Bean
  HttpClient httpClient() {
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
        .responseTimeout(Duration.of(20, ChronoUnit.SECONDS));
  }

  @Bean
  WebClientCustomizer webClientCustomizer() {
    return (webClientBuilder) ->
        webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient()));
  }
}
