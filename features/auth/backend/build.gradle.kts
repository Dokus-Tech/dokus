plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinxRpcPlugin)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "ai.dokus.users"
version = "1.0.0"

application {
    mainClass.set("ai.dokus.auth.backend.ApplicationKt")
}

kotlin {
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    implementation(projects.features.auth.domain)
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // KotlinX RPC Client & Server
    implementation(libs.kotlinx.rpc.core)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)

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

    // Ktor Client (for RPC)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

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
//    testImplementation(libs.kotlin.test.junit5)
//    testImplementation(libs.testcontainers)
//    testImplementation(libs.testcontainers.postgresql)
//    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "ai.dokus.auth.backend.ApplicationKt"
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