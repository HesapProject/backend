package uz.hesap.service.integration.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  // MyID client token cache — 55 minut (token muddati 1 soat).
  @Bean
  public Cache<String, String> cachedMyIdToken() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(55)).build();
  }

  // Eskiz auth token cache — EskizProvider 25 kungacha cache qiladi.
  @Bean
  public Cache<String, String> cachedEskiz() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(25)).build();
  }
}
