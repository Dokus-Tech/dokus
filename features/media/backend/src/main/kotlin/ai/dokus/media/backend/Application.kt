package ai.dokus.media.backend

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureJwtAuthentication
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.media.backend.config.configureDependencyInjection
import ai.dokus.media.backend.plugins.configureDatabase
import ai.dokus.media.backend.plugins.configureGracefulDatabaseShutdown
import ai.dokus.media.backend.plugins.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MediaApplication")

fun main() {
    val appConfig = AppBaseConfig.load()
    logger.info("Loaded configuration: ${appConfig.ktor.deployment.environment}")

    val server = embeddedServer(
        Netty,
        port = appConfig.ktor.deployment.port,
        host = appConfig.ktor.deployment.host,
    ) {
        module(appConfig)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down media server gracefully...")
        server.stop(5000, 10000)
        logger.info("Media server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    logger.info("Starting Media Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    configureDependencyInjection(appConfig)
    configureDatabase()

    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureJwtAuthentication()
    configureMonitoring()
    configureRouting()

    configureGracefulDatabaseShutdown()

    logger.info("Media Service started successfully")
}
