package uz.hesap.service.integration.context;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

  private final WebClient webClient;

  public SecurityConfig(
      final WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") final String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http.exceptionHandling(
            exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint(
                        (swe, e) ->
                            Mono.fromRunnable(
                                () -> swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                    .accessDeniedHandler(
                        (swe, e) ->
                            Mono.fromRunnable(
                                () -> swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN))))
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/swagger-ui.html")
                    .permitAll()
                    .pathMatchers("/swagger-ui/**")
                    .permitAll()
                    .pathMatchers("/integration/v3/api-docs/**")
                    .permitAll()
                    .pathMatchers("webjars/swagger-ui/**")
                    .permitAll()
                    .pathMatchers("/v3/api-docs/**")
                    .permitAll()
                    .pathMatchers("/integration/v1/local/**")
                    .permitAll()
                    // Lead-forma landing sahifadan JWT'siz keladi.
                    .pathMatchers(HttpMethod.POST, "/integration/v1/lid")
                    .permitAll()
                    // AbleID webhook — tashqi serverdan JWT'siz keladi (hash bilan tekshiriladi).
                    .pathMatchers(HttpMethod.POST, "/integration/v1/able-id/hook")
                    .permitAll()
                    // Payme/Click webhook'lari tashqi to'lov tizimidan JWT'siz keladi
                    // (Payme o'z Basic auth'ini yuboradi, controller'da tekshiriladi).
                    // Eski /billing/v1 yo'llari controller ko'chganda /integration/v1
                    // bo'lgan — permit yangilanmay webhook 401 olardi.
                    .pathMatchers(
                        HttpMethod.POST,
                        "/integration/v1/payme",
                        "/integration/v1/click/prepare",
                        "/integration/v1/click/complete")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .addFilterBefore(bearerAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(ServerHttpSecurity.CorsSpec::disable)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private AuthenticationWebFilter bearerAuthenticationFilter() {
    AuthenticationWebFilter bearerAuthenticationFilter;

    Function<ServerWebExchange, Mono<Authentication>> bearerConverter;

    ReactiveAuthenticationManager authManager;
    authManager = new CustomReactiveAuthenticationManager();
    bearerAuthenticationFilter = new AuthenticationWebFilter(authManager);
    bearerConverter = new JwtConverter(webClient);

    bearerAuthenticationFilter.setServerAuthenticationConverter(bearerConverter::apply);
    //
    // bearerAuthenticationFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/**"));
    return bearerAuthenticationFilter;
  }
}
