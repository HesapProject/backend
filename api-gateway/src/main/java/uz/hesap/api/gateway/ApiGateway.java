package uz.hesap.api.gateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(
    info =
        @Info(
            title = "Hesap API Gateway v1.0",
            version = "1.0",
            description = "Hesap API Gateway v1.0 Documentation"))
public class ApiGateway {

  public static void main(final String[] args) {
    SpringApplication.run(ApiGateway.class, args);
  }
}
