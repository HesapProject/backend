package uz.hesap.service.common.exception.handler;

import java.nio.charset.StandardCharsets;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * i18n konfiguratsiyasi — barcha servislarda avtomatik ishlatiladi. messages_uz.properties,
 * messages_ru.properties, messages_eng.properties fayllaridan ErrorCode.name() bo'yicha message
 * qaytaradi.
 */
@Configuration
public class MessageConfig {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:i18n/messages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setUseCodeAsDefaultMessage(true);
    return source;
  }
}
