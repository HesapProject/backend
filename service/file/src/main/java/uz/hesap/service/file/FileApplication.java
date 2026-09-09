package uz.hesap.service.file;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan("uz.hesap.service")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Hesap File Service v1.0",
            version = "1.0",
            description = "Hesap File Service v1.0 Documentation"),
    security = {@SecurityRequirement(name = "bearerAuth")},
    servers = {
      @Server(url = "http://localhost:8004/", description = "Local API Server URL"),
      @Server(url = "https://api.business.hesap.uz/", description = "Business API Server URL"),
      @Server(
          url = "https://api-dev.business.hesap.uz/",
          description = "Business Dev API Server URL"),
      @Server(url = "https://95.182.117.234:8004/", description = "Business API Server URL")
    })
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT auth description",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class FileApplication {

  public static void main(final String[] args) {
    SpringApplication.run(FileApplication.class, args);
  }
}
