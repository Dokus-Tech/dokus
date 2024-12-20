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
//
//jib {
//    from {
//        image = "openjdk:17-jdk-alpine"
//    }
//    to {
//        image = "bla"
//        tags = setOf("${project.version}")
//    }
//}

//ktor {
//    docker {
//        jreVersion.set(JavaVersion.VERSION_21)
//        localImageName.set("thepredict-gateway-image")
//        imageTag.set("${project.version}-preview")
//
//        externalRegistry.set(
//            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
//                appName = provider { "ktor-app" },
//                username = providers.environmentVariable("DOCKER_HUB_USERNAME"),
//                password = providers.environmentVariable("DOCKER_HUB_PASSWORD")
//            )
//        )
//
//        jib {
//            from {
//                image = "openjdk:17-jdk-alpine"
//            }
//            to {
//                image = "{DOCKER_HUB_USERNAME}/{DOCKER_HUB_REPOSITORY}"
//                tags = setOf("${project.version}")
//            }
//        }
//    }
//}

dependencies {
    implementation(projects.shared.api)
    implementation(projects.shared.configuration)
    implementation(projects.shared.domain)

    implementation(projects.server.contacts)
    implementation(projects.server.documentsApi)
    implementation(projects.server.identityApi)
    implementation(projects.server.predictionApi)
    implementation(projects.server.simulationApi)

    implementation(libs.logback)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.kotlinx.rpc.core)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)

//    implementation(libs.kotlinx.serialization)
//    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(kotlin("test"))
}