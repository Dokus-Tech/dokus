plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "ai.dokus.media"
version = "1.0.0"

application {
    mainClass.set("ai.dokus.media.backend.ApplicationKt")
}

kotlin {
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)
    implementation(projects.foundation.messaging)
    implementation(projects.foundation.database)

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

    // Database - Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    // Database - PostgreSQL
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Dependency Injection
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    // Security
    implementation(libs.java.jwt)

    // Logging
    implementation(libs.logback)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.core)

    // Database migrations
    implementation(libs.flyway.core)
    implementation(libs.flywaydb.flyway.database.postgresql)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.exposed.core)
    testImplementation(libs.exposed.dao)
    testImplementation(libs.exposed.jdbc)
    testImplementation(libs.exposed.kotlin.datetime)
    testImplementation(libs.hikaricp)
    testImplementation(libs.h2)
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "ai.dokus.media.backend.ApplicationKt"
    }
    mergeServiceFiles()
    archiveClassifier.set("")
    // Exclude duplicate files
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

// Disable unnecessary shadow tasks that conflict with regular tasks
tasks.named("startShadowScripts") {
    enabled = false
}

tasks.named("shadowDistTar") {
    enabled = false
}

tasks.named("shadowDistZip") {
    enabled = false
}

// Ensure build task runs shadowJar
tasks.named("build") {
    dependsOn("shadowJar")
}
