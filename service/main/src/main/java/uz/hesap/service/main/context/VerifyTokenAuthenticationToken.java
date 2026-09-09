package uz.hesap.service.main.context;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import uz.hesap.service.main.model.response.VerifySessionResponse;

public class VerifyTokenAuthenticationToken implements Authentication {

  private final VerifySessionResponse verifyDevice;
  private final String token;
  private Boolean authenticated = Boolean.TRUE;

  public VerifyTokenAuthenticationToken(VerifySessionResponse verifyDevice, String token) {
    this.verifyDevice = verifyDevice;
    this.token = token;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return verifyDevice;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws RuntimeException {
    this.authenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return verifyDevice.phone();
  }
}
