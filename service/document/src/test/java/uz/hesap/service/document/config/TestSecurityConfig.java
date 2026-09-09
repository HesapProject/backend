package uz.hesap.service.document.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** Test uchun security o'chiriladi — barcha endpoint larga ruxsat beriladi. */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

  @Bean
  @Primary
  public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
    return http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(ServerHttpSecurity.CorsSpec::disable)
        .build();
  }
}
