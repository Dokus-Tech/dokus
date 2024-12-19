plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "ai.thepredict.database"
version = "1.0.0"

dependencies {
    implementation(projects.shared.configuration)
    implementation(projects.shared.domain)

    implementation(libs.logback)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    testImplementation(libs.kotlin.test.junit)
}