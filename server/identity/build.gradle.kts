plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxRpcPlugin)
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

group = "ai.thepredict.identity"
version = "1.0.0"
application {
    mainClass.set("ai.thepredict.identity.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}


dependencies {
    implementation(projects.server.common)

    api(projects.shared.identityApi)
    implementation(projects.server.database)

    implementation(libs.logback)
//    testImplementation(libs.ktor.server.tests)

    implementation(libs.kotlinx.rpc.krpc.ktor.client)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(kotlin("test"))
}