apply(plugin = "org.springframework.boot")

dependencies {
    api(project(":service:common"))
    implementation(project(":service:jms"))

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.springframework.cloud:spring-cloud-starter-bus-amqp")

    implementation("net.sf.jasperreports:jasperreports:6.21.5")
    implementation("net.sf.jasperreports:jasperreports-fonts:6.21.5")
    implementation("com.google.zxing:core:3.5.0")
    implementation("com.google.zxing:javase:3.5.0")
    // Jasper PDF'ga QR + kirish kodini overlay qilish uchun (Jasper itext'iga tegmaydi).
    implementation("org.apache.pdfbox:pdfbox:2.0.31")

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
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("net.datafaker:datafaker:${rootProject.extra.get("fakerVersion")}")
}