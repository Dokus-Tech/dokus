package ai.dokus.auth.backend.plugins

import ai.dokus.auth.backend.routes.configureRoutes
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and REST API routes.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
    }
    configureRoutes()

    logger.info("Routes configured: health checks, REST API routes")
}
