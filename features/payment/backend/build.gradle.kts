plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "ai.dokus.payment"
version = "1.0.0"

application {
    mainClass.set("ai.dokus.payment.backend.ApplicationKt")
}

dependencies {
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)
    implementation(projects.foundation.database)

    implementation(libs.kotlinx.serialization)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)

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
            attributes["Main-Class"] = "ai.dokus.payment.backend.ApplicationKt"
        }
        archiveBaseName.set("backend")
        archiveVersion.set(project.version.toString())
        mergeServiceFiles()
        archiveClassifier.set("")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    // Fix task dependencies for application plugin
    named("distZip") {
        dependsOn("shadowJar")
    }
    named("distTar") {
        dependsOn("shadowJar")
    }
    named("startScripts") {
        dependsOn("shadowJar")
    }

    // Disable unnecessary shadow tasks that conflict with regular tasks
    named("startShadowScripts") {
        enabled = false
    }
    named("shadowDistTar") {
        enabled = false
    }
    named("shadowDistZip") {
        enabled = false
    }

    // Ensure build task runs shadowJar
    named("build") {
        dependsOn("shadowJar")
    }
}
