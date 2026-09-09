package uz.hesap.service.integration.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "application.my-id")
public class MyIdProperties {
  private String baseUrl;
  // MyID SDK uchun ikki credential juftligi: mobil ilova va web (business).
  private String mobileClientId;
  private String mobileClientSecret;
  private String webClientId;
  private String webClientSecret;
  private String redirectUri;
}
