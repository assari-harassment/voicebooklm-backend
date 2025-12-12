plugins {
    id("org.springframework.boot") version "3.4.12"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
}

group = "com.assari"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database Migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring Boot WebFlux（AI API との非同期通信用）
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring Security（認証・認可）
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // JWT 認証
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Google Cloud Speech-to-Text（音声文字起こし）
    implementation("com.google.cloud:google-cloud-speech:4.53.0")

    // UUID v7 生成（タイムオーダー UUID）
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    // API ドキュメント（Swagger/OpenAPI）
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

    // 開発環境
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // .env ファイル読み込み
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // Testcontainers（PostgreSQL でテスト）
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val integrationTest by tasks.register<Test>("integrationTest") {
    description = "Runs tests tagged with @Tag(\"integration\") (requires Docker for Testcontainers)."
    group = JavaBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTest)
}
