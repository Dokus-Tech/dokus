plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxRpcPlugin)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "ai.thepredict.common"

dependencies {
    api(projects.shared.configuration)
    api(projects.shared.domain)
    implementation(projects.server.database)

    api(libs.kotlinx.rpc.krpc.ktor.server)
    api(libs.kotlinx.rpc.core)
    api(libs.kotlinx.rpc.krpc.serialization.json)

    api(libs.ktor.server.core)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.netty)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
//    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}