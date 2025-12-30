plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "tech.dokus.backend"
version = "1.0.0"

application {
    mainClass.set("tech.dokus.backend.ApplicationKt")
}

kotlin {
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    // Foundation modules
    implementation(projects.foundation.domain)
    implementation(projects.foundation.backendCommon)
    implementation(projects.foundation.database)
    implementation(projects.foundation.peppolCore)

    // AI backend (kept as separate library)
    implementation(projects.features.ai.backend)

    // OCR module for text extraction
    implementation(projects.foundation.ocr)

    // Auth domain for shared types
    implementation(projects.features.auth.domain)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.resources)

    // Ktor Client (for external API calls)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)

    // Database - Exposed + PostgreSQL
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Dependency Injection
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    // Security
    implementation(libs.java.jwt)

    // Email
    implementation(libs.javax.mail)

    // Logging
    implementation(libs.logback)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.core)

    // Database migrations
    implementation(libs.flyway.core)
    implementation(libs.flywaydb.flyway.database.postgresql)

    // PDF/Image processing (for document processor)
    implementation(libs.pdfbox)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.h2)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "ai.dokus.backend.ApplicationKt"
    }
    mergeServiceFiles()
    archiveClassifier.set("all")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.named<Tar>("distTar") {
    dependsOn("shadowJar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Zip>("distZip") {
    dependsOn("shadowJar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("startScripts") {
    dependsOn("shadowJar")
}

tasks.named("startShadowScripts") {
    enabled = false
}

tasks.named("shadowDistTar") {
    enabled = false
}

tasks.named("shadowDistZip") {
    enabled = false
}

tasks.named("build") {
    dependsOn("shadowJar")
}
