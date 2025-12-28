plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "tech.dokus.ocr"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.kotlin.test)
}