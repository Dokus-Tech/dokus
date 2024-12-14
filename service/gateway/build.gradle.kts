plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
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
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
//    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}