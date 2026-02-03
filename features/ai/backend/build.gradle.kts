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
    implementation(libs.bundles.koog)

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
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
}

// =============================================================================
// Document Extraction E2E Tests
// =============================================================================

// Run extraction tests with mock AI (fast, CI-friendly)
tasks.register<Test>("extractionTestMock") {
    group = "verification"
    description = "Run extraction tests with mock AI (validates fixture structure only)"

    useJUnitPlatform()
    environment("EXTRACTION_TEST_MODE", "mock")
    filter {
        includeTestsMatching("*DocumentExtractionTest*")
    }
}

// Run extraction tests with real AI (slow, requires AI service)
tasks.register<Test>("extractionTestReal") {
    group = "verification"
    description = "Run extraction tests with real AI model (requires running AI service)"

    useJUnitPlatform()
    environment("EXTRACTION_TEST_MODE", "real")
    filter {
        includeTestsMatching("*DocumentExtractionTest*")
    }
}

// Record new fixture baselines
tasks.register<JavaExec>("recordExtractionFixtures") {
    group = "verification"
    description = "Run extraction on all fixtures and save results as new baselines"

    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("tech.dokus.features.ai.extraction.FixtureRecorderKt")

    environment("EXTRACTION_TEST_MODE", "record")

    // Allow filtering by fixture ID: -Pfixture=belgian-standard
    if (project.hasProperty("fixture")) {
        args(project.property("fixture").toString())
    }
}
