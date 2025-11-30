package ai.dokus.cashflow.backend.plugins

import ai.dokus.cashflow.backend.routes.configureCashflowRoutes
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and REST routes.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
    }
    configureCashflowRoutes()

    logger.info("Routes configured: health checks and REST routes")
}
