dependencies {
    api(project(":service:common"))

    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    api("org.springframework.boot:spring-boot-starter-amqp")

    // RabbitMQ Reactor
    api("io.projectreactor.rabbitmq:reactor-rabbitmq:1.5.6")

    // Log4J 2
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-web")
}