package ai.dokus.auth.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures health check routes.
 * Note: REST API routes are configured separately via configureRoutes() in Application.kt
 */
fun Application.configureRouting() {
    logger.info("Configuring health routes...")

    routing {
        healthRoutes()
    }

    logger.info("Health routes configured")
}
