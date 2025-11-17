package ai.dokus.auth.backend

import ai.dokus.auth.backend.config.configureDependencyInjection
import ai.dokus.auth.backend.plugins.configureBackgroundJobs
import ai.dokus.auth.backend.plugins.configureDatabase
import ai.dokus.auth.backend.plugins.configureGracefulDatabaseShutdown
import ai.dokus.auth.backend.plugins.configureRouting
import ai.dokus.auth.backend.plugins.configureRpc
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

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
        logger.info("Shutting down server gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    logger.info("Starting Dokus Auth Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Core configuration
    configureDependencyInjection(appConfig)
    configureDatabase()

    // Ktor plugins
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // RPC configuration
    configureRpc()

    // Application features
    configureBackgroundJobs()
    configureRouting()

    // Lifecycle management
    configureGracefulDatabaseShutdown()

    logger.info("Dokus Auth Service started successfully")
}