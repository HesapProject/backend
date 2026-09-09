package uz.hesap.service.main.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import uz.hesap.service.main.model.NewsResponse;
import uz.hesap.service.main.model.ClientCacheModel;
import uz.hesap.service.main.model.FaqResponse;
import uz.hesap.service.main.model.UserCacheModel;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

  @Bean
  public Cache<UUID, String> cachedSmsDigits() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(3)).build();
  }

  @Bean
  public Cache<String, UserCacheModel> cachedUserOnSignUp() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(3)).build();
  }

  @Bean
  public Cache<String, String> cachedPhoneVerifyCode() {
    return Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(2))
        .maximumSize(100_000)
        .build();
  }

  @Bean
  public Cache<String, String> cachedEskiz() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(25)).build();
  }

  // CMS keshlari (notification servisidan ko'chirildi)
  @Bean
  public Cache<String, Page<NewsResponse>> cacheNews() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).build();
  }

  @Bean
  public Cache<String, List<FaqResponse>> faqCache() {
    return Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).maximumSize(100).build();
  }

  /**
   * Cache for client (C2C) authentication flow Key: phone number Value: ClientCacheModel with user
   * data and verification code
   */
  @Bean
  public Cache<String, ClientCacheModel> cachedClientAuth() {
    return Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(2)) // 10 minutes for SMS code expiry
        .maximumSize(10_000)
        .build();
  }
}
