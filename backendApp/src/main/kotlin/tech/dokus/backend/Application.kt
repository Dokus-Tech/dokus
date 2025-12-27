package tech.dokus.backend

import tech.dokus.foundation.ktor.config.AppBaseConfig
import tech.dokus.foundation.ktor.configure.configureErrorHandling
import tech.dokus.foundation.ktor.configure.configureJwtAuthentication
import tech.dokus.foundation.ktor.configure.configureMonitoring
import tech.dokus.foundation.ktor.configure.configureSecurity
import tech.dokus.foundation.ktor.configure.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.resources.Resources
import org.slf4j.LoggerFactory
import tech.dokus.backend.config.configureDependencyInjection
import tech.dokus.backend.plugins.configureBackgroundWorkers
import tech.dokus.backend.plugins.configureDatabase
import tech.dokus.backend.plugins.configureGracefulDatabaseShutdown
import tech.dokus.backend.plugins.configureRouting

private val logger = LoggerFactory.getLogger("DokusServer")

fun main() {
    val appConfig = _root_ide_package_.tech.dokus.foundation.ktor.config.AppBaseConfig.load()
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

fun Application.module(appConfig: tech.dokus.foundation.ktor.config.AppBaseConfig) {
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
