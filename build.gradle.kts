import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    // https://github.com/spring-gradle-plugins/dependency-management-plugin/releases
    id("io.spring.dependency-management") version "1.1.7"

    // https://github.com/spring-projects/spring-boot/releases
    id("org.springframework.boot") version "3.4.11" apply false

    // https://github.com/n0mer/gradle-git-properties/releases
    id("com.gorylenko.gradle-git-properties") version "2.5.3"

    // https://github.com/researchgate/gradle-release
    // https://mvnrepository.com/artifact/net.researchgate.release/net.researchgate.release.gradle.plugin
//    id("net.researchgate.release") version "3.0.2"

    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.53.0"

    // https://github.com/freefair/gradle-plugins/releases
    id("io.freefair.lombok") version "9.1.0"

    java
    idea
}

allprojects {
    apply(plugin = "io.spring.dependency-management")

    group = "uz.hesap.service"
    description = "Hesap Project Source"

    repositories {
        mavenLocal()
        mavenCentral()
        maven{url = uri("https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/")}
    }

    dependencyManagement {
        imports {
            // https://github.com/spring-projects/spring-boot/releases
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.11")

            // https://spring.io/projects/spring-cloud
            // https://github.com/spring-cloud/spring-cloud-release/tags
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.2")

            // To avoid specifying the version of each dependency, use a BOM or Bill Of Materials.
            // https://github.com/testcontainers/testcontainers-java/releases
            mavenBom("org.testcontainers:testcontainers-bom:1.20.4")

            //https://immutables.github.io/
            mavenBom("org.immutables:bom:2.11.6")
        }

        dependencies {
            // https://github.com/apache/logging-log4j2/tags
            dependencySet("org.apache.logging.log4j:2.25.2") {
                entry("log4j-core")
                entry("log4j-api")
                entry("log4j-web")
            }
        }
    }
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
}
// Find more about multi-project setup: https://docs.gradle.org/current/userguide/multi_project_builds.html
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "com.gorylenko.gradle-git-properties")
//    apply(plugin = "net.researchgate.release")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "io.freefair.lombok")

    configurations {
        all {
            exclude("org.springframework.boot", "spring-boot-starter-logging")

            // Can't exclude because of this: https://github.com/testcontainers/testcontainers-java/issues/970
            // exclude("junit", "junit")
        }
    }

    dependencies {
        annotationProcessor("org.immutables:value")
        compileOnly("org.immutables:builder")
        compileOnly("org.immutables:value-annotations")

        // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#configuration-metadata-annotation-processor
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

        // https://mapstruct.org/documentation/installation/
        annotationProcessor("org.mapstruct:mapstruct-processor:${rootProject.extra.get("mapStructVersion")}")
    }

    tasks.compileJava {
        dependsOn("processResources")
        options.release.set(21)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    }

    tasks.processResources {
        val tokens = mapOf(
                "application.version" to project.version,
                "application.description" to project.description
        )
        filesMatching("**/*.yml") {
            filter<ReplaceTokens>("tokens" to tokens)
        }
    }

    tasks.test {
        failFast = false
        enableAssertions = true

        // Enable JUnit 5 (Gradle 4.6+).
        useJUnitPlatform()

        testLogging {
            events("PASSED", "STARTED", "FAILED", "SKIPPED")
            // Set to true if you want to see output from tests
            showStandardStreams = false
            setExceptionFormat("FULL")
        }

        systemProperty("io.netty.leakDetectionLevel", "paranoid")
    }
}

defaultTasks("build")
