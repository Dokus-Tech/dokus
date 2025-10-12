package ai.dokus.foundation.database

import ai.dokus.foundation.database.configuration.configureDependencyInjection
import ai.dokus.foundation.database.tables.UserLoginAttemptsTable
import ai.dokus.foundation.database.tables.UserPermissionsTable
import ai.dokus.foundation.database.tables.UserRolesTable
import ai.dokus.foundation.database.tables.UserSessionsTable
import ai.dokus.foundation.database.tables.UserSpecializationsTable
import ai.dokus.foundation.database.tables.UsersTable
import ai.dokus.foundation.database.utils.DatabaseFactory
import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val appConfig = AppConfig.load()

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

fun Application.module(appConfig: AppConfig) {
    // Log application startup
    logger.info("Starting Dokus Database Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Configure application
    configureDependencyInjection(appConfig)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // Initialize database
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        dbFactory.init(
            UsersTable,
            UserSessionsTable,
            UserLoginAttemptsTable,
            UserRolesTable,
            UserPermissionsTable,
            UserSpecializationsTable
        )
    }

    // Configure routes
    routing {
        healthRoutes()
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        runBlocking {
            val dbFactory by inject<DatabaseFactory>()
            // Close database connections
            dbFactory.close()
        }
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Database Service started successfully")
}