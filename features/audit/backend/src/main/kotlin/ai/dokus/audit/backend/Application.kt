package ai.dokus.audit.backend

import ai.dokus.audit.backend.config.configureDependencyInjection
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.services.AuditService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
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
    logger.info("Starting Dokus Audit Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Configure application
    configureDependencyInjection(appConfig)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // Install KotlinX RPC plugin
    install(Krpc)

    // Configure routes
    routing {
        healthRoutes()

        // Register RPC service
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<AuditService> { get<AuditService>() }
        }

        logger.info("AuditService registered at /api/rpc")
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Audit Service started successfully")
}
