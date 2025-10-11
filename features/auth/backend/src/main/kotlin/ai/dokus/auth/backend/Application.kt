package ai.dokus.auth.backend

import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.db.DatabaseFactory
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.auth.backend.config.configureAuthentication
import ai.dokus.auth.backend.config.configureDependencyInjection
import ai.dokus.auth.backend.database.tables.UserLoginAttemptsTable
import ai.dokus.auth.backend.database.tables.UserPermissionsTable
import ai.dokus.auth.backend.database.tables.UserRolesTable
import ai.dokus.auth.backend.database.tables.UserSessionsTable
import ai.dokus.auth.backend.database.tables.UserSpecializationsTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.auth.backend.routes.identityRoutes
import ai.dokus.auth.backend.routes.passwordlessAuthRoutes
import ai.dokus.auth.backend.routes.userRoutes
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
    logger.info("Starting Pulse Auth Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Configure application
    configureDependencyInjection(appConfig)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureAuthentication(appConfig)
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
        identityRoutes()
        userRoutes()
        passwordlessAuthRoutes()
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

    logger.info("Pulse Auth Service started successfully")
}