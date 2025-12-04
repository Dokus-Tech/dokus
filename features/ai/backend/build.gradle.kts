plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "ai.dokus.ai"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    // JetBrains Koog AI Framework
    implementation(libs.koog.agents)

    // Foundation dependencies
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)

    // Kotlin
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    // Koin DI
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutinesTest)
}

tasks.test {
    useJUnitPlatform()
}
