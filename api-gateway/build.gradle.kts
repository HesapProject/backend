apply(plugin = "org.springframework.boot")

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
//    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Caffeine Cache
    // Spring Cloud LoadBalancer uses Caffeine Cache if it is present
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Swagger Open API
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${rootProject.extra.get("openApiVersion")}")

    // Log4J 2
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-web")

    // Test
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}