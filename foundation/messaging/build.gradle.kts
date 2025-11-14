plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "ai.dokus.foundation.messaging"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    // Foundation dependencies
    implementation(projects.foundation.domain)
    implementation(projects.foundation.platform)

    // Kotlin
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // Koin DI
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    // Messaging queue (RabbitMQ + Resilience4j)
    implementation(libs.bundles.messaging.queue)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
}

tasks.test {
    useJUnitPlatform()
}
