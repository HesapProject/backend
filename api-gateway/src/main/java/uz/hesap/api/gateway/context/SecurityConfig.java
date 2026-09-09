package uz.hesap.api.gateway.context;

// import lombok.extern.log4j.Log4j2;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.Customizer;
// import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
// import org.springframework.security.config.web.server.ServerHttpSecurity;
// import org.springframework.security.web.server.SecurityWebFilterChain;

// @Log4j2
// @Configuration
// @EnableWebFluxSecurity
// public class SecurityConfig {
//
//  @Bean
//  public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http) {
//    return http.authorizeExchange(
//            exchangeSpec -> exchangeSpec.pathMatchers("/api/main/v1/**").permitAll())
//        .authorizeExchange(exchangeSpec -> exchangeSpec.anyExchange().authenticated())
//        .oauth2Login(Customizer.withDefaults())
//        .csrf(ServerHttpSecurity.CsrfSpec::disable)
//        .build();
//  }
// }
