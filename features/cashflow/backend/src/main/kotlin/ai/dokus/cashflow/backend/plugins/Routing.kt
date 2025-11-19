package ai.dokus.cashflow.backend.plugins

import ai.dokus.cashflow.backend.withRemoteServices
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and RPC services.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        withRemoteServices()
    }

    logger.info("Routes configured: health checks and RPC services")
    logger.info("RPC API available at /rpc")
}
