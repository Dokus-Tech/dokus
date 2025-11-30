package ai.dokus.payment.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.payment.backend.routes.paymentRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and payment routes.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        paymentRoutes()
    }

    logger.info("Routes configured: health checks, payment routes")
}
