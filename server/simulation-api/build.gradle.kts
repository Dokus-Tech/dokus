plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "ai.thepredict.simulation.api"
version = "1.0.0"

dependencies {
    implementation(projects.shared.domain)
}