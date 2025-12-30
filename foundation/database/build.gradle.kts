plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "tech.dokus.database"
version = "1.0.0"

kotlin {
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    // Foundation modules
    implementation(projects.foundation.domain)
    implementation(projects.foundation.backendCommon)

    // DI - Koin (with BOM for version management)
    api(platform(libs.koin.bom))
    api(libs.koin.core)

    // Serialization
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // Database - Exposed
    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.kotlin.datetime)

    // Database - PostgreSQL & Connection Pool
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Coroutines
    implementation(libs.kotlinx.coroutinesCore)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.h2) // In-memory database for testing
}
