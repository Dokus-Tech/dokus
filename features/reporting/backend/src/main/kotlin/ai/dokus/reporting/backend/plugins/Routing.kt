package ai.dokus.reporting.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.reporting.backend.routes.reportRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and report routes.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        reportRoutes()
    }

    logger.info("Routes configured: health checks, report routes")
}
