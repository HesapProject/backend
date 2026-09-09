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
    implementation("org.springframework.cloud:spring-cloud-starter-bus-amqp")

    // Click MD5 imzosini tekshirish uchun (DigestUtils)
    implementation("commons-codec:commons-codec:1.16.0")

    // Firebase Admin SDK (FCM push — notification servisidan ko'chirildi)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    // OAuth (GoogleCredentials — service account uchun)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Google Sheets (lead-forma — notification servisidan ko'chirildi)
    implementation("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0")
    implementation("com.google.api-client:google-api-client:1.35.2")
    implementation("com.google.http-client:google-http-client:1.45.0")
    implementation("com.google.http-client:google-http-client-gson:1.45.0")

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
    testImplementation("net.datafaker:datafaker:${rootProject.extra.get("fakerVersion")}")
}