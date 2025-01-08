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
    api(libs.exposed.dao) // TODO No api
    implementation(libs.bundles.database)

    testImplementation(libs.kotlin.test.junit)
}