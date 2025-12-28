plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "tech.dokus.ocr"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    implementation(libs.kotlinx.serialization)

    // MinIO object storage
    implementation(libs.minio)

    // Image processing for avatar resizing
    implementation(libs.scrimage.core)
    implementation(libs.scrimage.webp)
}