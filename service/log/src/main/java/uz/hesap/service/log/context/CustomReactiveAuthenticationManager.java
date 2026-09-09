package uz.hesap.service.log.context;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public class CustomReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return Mono.just(authentication);
  }
}
