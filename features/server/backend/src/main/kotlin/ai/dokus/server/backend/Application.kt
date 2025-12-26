package ai.dokus.server.backend

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureJwtAuthentication
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.server.backend.config.configureDependencyInjection
import ai.dokus.server.backend.plugins.configureBackgroundWorkers
import ai.dokus.server.backend.plugins.configureDatabase
import ai.dokus.server.backend.plugins.configureGracefulDatabaseShutdown
import ai.dokus.server.backend.plugins.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.resources.Resources
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DokusServer")

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
        logger.info("Shutting down Dokus Server gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    logger.info("Starting Dokus Server (modular monolith)...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")
    logger.info("Port: ${appConfig.ktor.deployment.port}")

    // Dependency injection first (JwtValidator, DatabaseFactory, etc)
    configureDependencyInjection(appConfig)
    configureDatabase()

    // Ktor plugins
    install(Resources)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // JWT Authentication
    configureJwtAuthentication()

    // Routes
    configureRouting(appConfig)

    // Lifecycle management
    configureBackgroundWorkers(appConfig)
    configureGracefulDatabaseShutdown()

    logger.info("Dokus Server started successfully")
}
