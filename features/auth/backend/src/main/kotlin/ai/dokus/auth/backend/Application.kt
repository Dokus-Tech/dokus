package ai.dokus.auth.backend

import ai.dokus.auth.backend.config.configureDependencyInjection
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.middleware.RpcAuthPlugin
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
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
    // Log application startup
    logger.info("Starting Dokus Auth Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Configure application
    configureDependencyInjection(appConfig)

    // Initialize database
    logger.info("Initializing database connection...")
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        // Database initialization happens in the DatabaseFactory constructor via Koin
        // This line triggers the lazy initialization
        logger.info("Database initialized successfully: ${dbFactory.database}")
    }

    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // Install KotlinX RPC plugin
    install(Krpc)

    // Install RPC authentication plugin
    val jwtValidator = get<JwtValidator>()
    attributes.put(AttributeKey("JwtValidator"), jwtValidator)
    install(RpcAuthPlugin)

    // Start background jobs
    val rateLimitCleanupJob = get<RateLimitCleanupJob>()
    rateLimitCleanupJob.start()
    logger.info("Started rate limit cleanup job")

    // Configure routes
    routing {
        healthRoutes()
        withRemoteServices()
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        runBlocking {
            // Close database connections
            val dbFactory by inject<DatabaseFactory>()
            dbFactory.close()
            logger.info("Database connections closed")
        }
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Auth Service started successfully")
}