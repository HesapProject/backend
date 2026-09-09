package uz.hesap.service.integration.context;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleSheetsConfig {

  @Value("${google.credentials.path}")
  private String credentialsPath;

  @Bean
  public Sheets sheets() throws Exception {

    GoogleCredentials credentials;
    try (var inputStream = Files.newInputStream(Paths.get(credentialsPath))) {
      credentials =
          GoogleCredentials.fromStream(inputStream)
              .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));
    }

    return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials))
        .setApplicationName("lead-app")
        .build();
  }
}
