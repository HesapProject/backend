package uz.hesap.api.gateway.context;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    config.addAllowedHeader("origin");
    config.addAllowedHeader("content-type");
    config.addAllowedHeader("accept");
    config.addAllowedHeader("authorization");
    config.addAllowedHeader("cookie");
    config.addAllowedHeader("file");
    config.addExposedHeader("file");
    config.addExposedHeader("content-disposition");

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
