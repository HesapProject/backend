package uz.hesap.service.document.context;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uz.hesap.service.common.util.UserPrincipal;

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
    // role endi null bo'lishi mumkin (avtorizatsiya `type`ga ko'chgan) — null'larni
    // filtrlaymiz, aks holda role().name() har so'rovda NPE berib butun servis 500 berardi.
    super(
        Stream.of(
                userPrincipal.user().role() != null ? userPrincipal.user().role().name() : null,
                userPrincipal.user().type() != null ? userPrincipal.user().type().name() : null)
            .filter(Objects::nonNull)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet()));
    this.userPrincipal = userPrincipal;
    this.token = userPrincipal.token();
    super.setAuthenticated(true);
  }
}
