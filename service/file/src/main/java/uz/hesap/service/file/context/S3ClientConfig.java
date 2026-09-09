package uz.hesap.service.file.context;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class S3ClientConfig {

  @Bean
  public S3AsyncClient s3AsyncClient(CdnProperties props) {
    return S3AsyncClient.builder()
        .endpointOverride(URI.create(props.getEndpoint()))
        .region(Region.of(props.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
        .forcePathStyle(Boolean.TRUE)
        .build();
  }
}
