plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "ai.dokus.foundation.ktor"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.foundation.domain)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
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
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.hsts)
    implementation(libs.ktor.server.request.validation)

    // Database - Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    // Database - PostgreSQL
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Redis presentation for distributed caching
    implementation(libs.lettuce.core)
    implementation(libs.commons.pool2)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.core)

    // Database migrations
    implementation(libs.flyway.core)
    implementation(libs.flywaydb.flyway.database.postgresql)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    implementation(libs.kotlinx.serialization)

    implementation(libs.password4j)

    // MinIO object storage
    implementation(libs.minio)
}