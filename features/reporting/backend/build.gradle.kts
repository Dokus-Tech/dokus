plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinxRpcPlugin)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "ai.dokus.reporting"
version = "1.0.0"

application {
    mainClass.set("ai.dokus.reporting.backend.ApplicationKt")
}

dependencies {
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)
    implementation(projects.foundation.domain)

    implementation(libs.kotlinx.serialization)

    // KotlinX RPC
    implementation(libs.kotlinx.rpc.core)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)

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

    // Database migrations
    implementation(libs.flyway.core)
    implementation(libs.flywaydb.flyway.database.postgresql)

    // Dependency Injection
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    // Logging
    implementation(libs.logback)
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "ai.dokus.reporting.backend.ApplicationKt"
        }
        archiveBaseName.set("backend")
        archiveVersion.set(project.version.toString())
        mergeServiceFiles()
        archiveClassifier.set("")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
}
