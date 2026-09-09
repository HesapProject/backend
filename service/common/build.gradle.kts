dependencies {
  // Spring
//    implementation("org.springframework:spring-web")
//    implementation("org.springframework:spring-context")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.data:spring-data-commons")

  // Log4J 2
  implementation("org.apache.logging.log4j:log4j-api")
  implementation("org.apache.logging.log4j:log4j-core")
  implementation("org.apache.logging.log4j:log4j-web")
  implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")

}