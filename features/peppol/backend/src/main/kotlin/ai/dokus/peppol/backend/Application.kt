package ai.dokus.peppol.backend

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureJwtAuthentication
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.peppol.backend.config.configureDependencyInjection
import ai.dokus.peppol.backend.plugins.*
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PeppolApplication")

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

    // Setup shutdown hook for graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Peppol Service gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    logger.info("Starting Dokus Peppol Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")
    logger.info("Port: ${appConfig.ktor.deployment.port}")

    // Core configuration
    configureDependencyInjection(appConfig)
    configureDatabase()

    // Ktor plugins
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureJwtAuthentication()
    configureMonitoring()

    // Application features
    configureRouting()

    // Lifecycle management
    configureGracefulDatabaseShutdown()

    logger.info("Dokus Peppol Service started successfully on port ${appConfig.ktor.deployment.port}")
}
