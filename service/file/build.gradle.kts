apply(plugin = "org.springframework.boot")

dependencies {
  api(project(":service:common"))

  // Spring
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-log4j2")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  // PostgresSQL
  runtimeOnly("org.postgresql:postgresql")
  implementation("org.postgresql:r2dbc-postgresql")

  // Liquibase
  implementation("org.liquibase:liquibase-core")
  implementation("org.springframework:spring-jdbc")
  // Caffeine Cache
  // Spring Cloud LoadBalancer uses Caffeine Cache if it is present
  implementation("com.github.ben-manes.caffeine:caffeine")

  // MapStruct
  implementation("org.mapstruct:mapstruct:${rootProject.extra.get("mapStructVersion")}")


  // Swagger Open API
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${rootProject.extra.get("openApiVersion")}")

  // AWS S3 SDK (for DigitalOcean Spaces CDN)
  implementation("software.amazon.awssdk:s3:2.25.16")

  // Test
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:mockserver")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mock-server:mockserver-junit-jupiter:${rootProject.extra.get("mockServerVersion")}")
  testImplementation("net.datafaker:datafaker:${rootProject.extra.get("fakerVersion")}")
}