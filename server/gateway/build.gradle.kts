plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxRpcPlugin)
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

group = "ai.thepredict.gateway"
version = "1.0.0"
application {
    mainClass.set("ai.thepredict.gateway.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared.configuration)
    implementation(projects.shared.domain)

    implementation(projects.server.common)
    implementation(projects.server.contacts)
    implementation(projects.server.documents)
    implementation(projects.server.identity)
    implementation(projects.server.prediction)
    implementation(projects.server.simulation)

    implementation(libs.logback)

    implementation(libs.ktor.server.config.yaml)

    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)

//    implementation(libs.kotlinx.serialization)
//    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(kotlin("test"))
}