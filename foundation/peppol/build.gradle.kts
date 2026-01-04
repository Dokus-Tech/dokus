plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "tech.dokus.peppol"
version = "1.0.0"

dependencies {
    // Foundation modules
    implementation(projects.foundation.domain)
    implementation(projects.foundation.database)
    implementation(projects.foundation.backendCommon)

    // Configuration (HOCON)
    implementation(libs.hoconConfig)

    // Kotlin
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutinesCore)

    // Ktor Client (for provider HTTP calls)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
