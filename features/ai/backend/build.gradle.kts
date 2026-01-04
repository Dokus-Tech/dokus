plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    `java-library`
}

group = "tech.dokus.features.ai"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        suppressWarnings.set(true)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

dependencies {
    // JetBrains Koog AI Framework (api to expose PromptExecutor, LLModel to dependents)
    api(libs.koog.agents)

    // Foundation dependencies
    implementation(projects.foundation.domain)
    implementation(projects.foundation.backendCommon)

    // PDFBox for PDF to image conversion (vision processing)
    implementation(libs.pdfbox)

    // Ktor HTTP Client (for Ollama/OpenAI embeddings API)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

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
