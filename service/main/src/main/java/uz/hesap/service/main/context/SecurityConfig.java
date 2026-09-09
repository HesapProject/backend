package uz.hesap.service.main.context;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.JwtService;
import uz.hesap.service.main.service.UserService;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtService jwtService;
  private final UserService userService;
  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;

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
                    .pathMatchers("/main/v1/users/test")
                    .permitAll()
                    // Hujjatlar (about/terms/privacy) o'qish — ochiq; yangilash (PUT) admin.
                    .pathMatchers(org.springframework.http.HttpMethod.GET, "/main/v1/docs/**")
                    .permitAll()
                    .pathMatchers("/main/v1/local/**")
                    .permitAll()
                    // Barcha login oqimlari (admin login, OneID, E-IMZO) public
                    .pathMatchers("/main/v1/auth/**")
                    .permitAll()
                    // Other auth endpoints are public
                    .pathMatchers(
                        "/main/v1/users/check",
                        "/main/v1/users/sign-up",
                        "/main/v1/users/sign-up/confirm",
                        "/main/v1/users/recovery",
                        "/main/v1/users/recovery/confirm",
                        "/main/v1/users/google/auth")
                    .permitAll()
                    .pathMatchers("/main/v1/driver/login/check")
                    .permitAll()
                    .pathMatchers("/main/v1/users/forgot/**")
                    .permitAll()
                    .pathMatchers("/swagger-ui.html")
                    .permitAll()
                    .pathMatchers("/swagger-ui/**")
                    .permitAll()
                    .pathMatchers("/users/v3/api-docs/**")
                    .permitAll()
                    .pathMatchers("/webjars/swagger-ui/**")
                    .permitAll()
                    .pathMatchers("/v3/api-docs/**")
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
    bearerConverter =
        new JwtConverter(jwtService, userService, sessionRepository, userRepository);

    bearerAuthenticationFilter.setServerAuthenticationConverter(bearerConverter::apply);
    //
    // bearerAuthenticationFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/**"));
    return bearerAuthenticationFilter;
  }
}
