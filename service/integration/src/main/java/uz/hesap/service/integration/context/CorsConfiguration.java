package uz.hesap.service.integration.context;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfiguration implements WebFluxConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry corsRegistry) {
    corsRegistry
        .addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("*")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
