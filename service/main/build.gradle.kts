apply(plugin = "org.springframework.boot")

dependencies {
    api(project(":service:common"))
    api(project(":service:jms"))

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.springframework.cloud:spring-cloud-starter-bus-amqp")

    // https://mvnrepository.com/artifact/com.google.api-client/google-api-client
    implementation("com.google.api-client:google-api-client:2.8.0")
    // https://mvnrepository.com/artifact/com.google.auth/google-auth-library-oauth2-http
    implementation("com.google.auth:google-auth-library-oauth2-http:1.35.0")
    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Caffeine Cache
    // Spring Cloud LoadBalancer uses Caffeine Cache if it is present
    implementation("com.github.ben-manes.caffeine:caffeine")

    // MapStruct
    implementation("org.mapstruct:mapstruct:${rootProject.extra.get("mapStructVersion")}")

    // PostgresSQL
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.postgresql:r2dbc-postgresql")

    // Liquibase
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework:spring-jdbc")

    // Swagger Open API
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${rootProject.extra.get("openApiVersion")}")

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