package ai.dokus.cashflow.backend

import ai.dokus.cashflow.backend.config.configureDependencyInjection
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CashflowApplication")

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
        logger.info("Shutting down Cashflow Service gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    // Log application startup
    logger.info("Starting Dokus Cashflow Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")
    logger.info("Port: ${appConfig.ktor.deployment.port}")

    // Configure application
    configureDependencyInjection(appConfig)

    // Initialize database
    logger.info("Initializing database connection...")
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        logger.info("Database initialized successfully: ${dbFactory.database}")
    }

    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // Install KotlinX RPC plugin
    install(Krpc)

    // Configure routes
    routing {
        healthRoutes()

        // Register RPC API
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            // Register CashflowApi service
            registerService<CashflowApi> { get<CashflowApi>() }
        }
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        runBlocking {
            val dbFactory by inject<DatabaseFactory>()
            dbFactory.close()
            logger.info("Database connections closed")
        }
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Cashflow Service started successfully on port ${appConfig.ktor.deployment.port}")
    logger.info("RPC API available at /rpc")
    logger.info("Health check available at /health")
}
