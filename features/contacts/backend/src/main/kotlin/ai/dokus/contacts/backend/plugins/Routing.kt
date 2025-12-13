package ai.dokus.contacts.backend.plugins

import ai.dokus.contacts.backend.routes.contactRoutes
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
        contactRoutes()
    }

    logger.info("Routes configured: health checks and contacts REST routes")
}
