plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "ai.thepredict.identity.api"
version = "1.0.0"

dependencies {
    implementation(projects.shared.domain)
}