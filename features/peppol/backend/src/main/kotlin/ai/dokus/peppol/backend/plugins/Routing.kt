package ai.dokus.peppol.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.peppol.backend.routes.peppolRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and Peppol REST routes.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        peppolRoutes()
    }

    logger.info("Routes configured: health checks and Peppol REST routes")
}
