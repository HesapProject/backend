package uz.hesap.service.file.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cdn.digitalocean")
public class CdnProperties {
  private String accessKey;
  private String secretKey;
  private String bucket = "cdn-hesap";
  private String region = "fra1";
  private String endpoint = "https://fra1.digitaloceanspaces.com";

  /**
   * Public URL base — fayllarni qaytarish uchun. \`cdn.hesap.uz\` subdomeni nginx reverse-proxy
   * orqali file service'ga (\`file:8004\`) yo'naltirilgan. URL rewrite:
   * \`cdn.hesap.uz/{folder}/{filename}\` → \`file:8004/api/files/v1/cdn/{folder}/{filename}\`. Bo'sh
   * bo'lsa, native DO Spaces URL ishlatiladi (legacy backward-compat).
   */
  private String publicBaseUrl = "";

  public String getPublicUrl(String folder, String fileName) {
    if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
      String base =
          publicBaseUrl.endsWith("/")
              ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
              : publicBaseUrl;
      return String.format("%s/%s/%s", base, folder, fileName);
    }
    return String.format(
        "https://%s.%s.digitaloceanspaces.com/%s/%s", bucket, region, folder, fileName);
  }
}
