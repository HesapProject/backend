package uz.hesap.service.log.context;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.Role;

public class CurrentUserAuthenticationToken extends AbstractAuthenticationToken
    implements Serializable {

  private final UserPrincipal userPrincipal;
  private final String token;

  @Override
  public Object getCredentials() {
    return this.token;
  }

  @Override
  public Object getPrincipal() {
    return this.userPrincipal;
  }

  public CurrentUserAuthenticationToken(UserPrincipal userPrincipal) {
    super(
        Stream.of(
                userPrincipal.user().role() == null
                    ? Role.USER.name()
                    : userPrincipal.user().role().name())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet()));
    this.userPrincipal = userPrincipal;
    this.token = userPrincipal.token();
    super.setAuthenticated(true);
  }
}
