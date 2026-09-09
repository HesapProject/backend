package uz.hesap.service.integration.context;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// FCM credential — firebase.json classpath resource (notification servisidan ko'chirildi).
@Configuration
public class FirebaseConfig {

  @Bean
  FirebaseApp firebaseApp() throws IOException {

    InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase.json");
    assert serviceAccount != null;
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

    return FirebaseApp.initializeApp(options);
  }
}
