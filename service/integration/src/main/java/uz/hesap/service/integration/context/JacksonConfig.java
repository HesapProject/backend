package uz.hesap.service.integration.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class JacksonConfig implements WebFluxConfigurer {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().build();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return objectMapper;
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    ObjectMapper objectMapper = objectMapper();
    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
  }
}
