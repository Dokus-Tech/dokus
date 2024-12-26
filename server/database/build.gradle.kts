plugins {
    alias(libs.plugins.kotlinJvm)
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
    api(libs.exposed.dao)
    implementation(libs.exposed.datetime)

    testImplementation(libs.kotlin.test.junit)
}