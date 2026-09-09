package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.model.myid.MyIdPlatform;
import uz.hesap.service.integration.util.Constants;

// MyID credential'larini admin orqali boshqarish uchun singleton table.
// DB qatori bo'lmasa, MyIdService application.yml'dagi qiymatlarga qaytadi.
// Mobil va web SDK uchun alohida client juftliklari.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_MY_ID_SETTING)
public class MyIdSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String mobileClientId;
  private String mobileClientSecret;
  private String webClientId;
  private String webClientSecret;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();

  // Platformaga mos client id/secret — WEB bo'lsa web juftligi, aks holda mobile.
  public String clientId(MyIdPlatform platform) {
    return platform == MyIdPlatform.WEB ? webClientId : mobileClientId;
  }

  public String clientSecret(MyIdPlatform platform) {
    return platform == MyIdPlatform.WEB ? webClientSecret : mobileClientSecret;
  }
}
