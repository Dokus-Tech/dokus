plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxRpcPlugin)
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

group = "ai.thepredict.website"
version = "1.0.0"
application {
    mainClass.set("ai.thepredict.website.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared.configuration)
    implementation(projects.shared.domain)

    implementation(projects.server.common)

    implementation(libs.logback)

    implementation(libs.ktor.server.config.yaml)

    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)

//    implementation(libs.kotlinx.serialization)
//    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(kotlin("test"))
}