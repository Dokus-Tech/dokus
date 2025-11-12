package ai.dokus.invoicing.backend

import ai.dokus.foundation.domain.rpc.InvoiceApi
import ai.dokus.invoicing.backend.config.configureDependencyInjection
import ai.dokus.invoicing.backend.routes.invoiceRoutes
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.routes.healthRoutes
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
    logger.info("Starting Dokus Invoicing Service...")
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
        invoiceRoutes()

        // Register RPC APIs
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<InvoiceApi> { get<InvoiceApi>() }
        }

        logger.info("RPC APIs registered at /api")
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Invoicing Service started successfully")
}
