package uz.hesap.service.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("junit")
@ExtendWith(SpringExtension.class)
public abstract class AbstractSpringBootTest {

  private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

  @Autowired protected ObjectMapper objectMapper;

  static {
    // https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
    POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:15.1");
    POSTGRESQL_CONTAINER.withInitScript("create-process-schema.sql");
    POSTGRESQL_CONTAINER.start();
  }

  @DynamicPropertySource
  static void initialize(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);

    registry.add("spring.liquibase.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add("spring.liquibase.user", POSTGRESQL_CONTAINER::getUsername);
    registry.add("spring.liquibase.password", POSTGRESQL_CONTAINER::getPassword);
  }
}
