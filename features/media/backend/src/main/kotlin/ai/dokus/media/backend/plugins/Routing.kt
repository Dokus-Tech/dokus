package ai.dokus.media.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.media.backend.routes.mediaRoutes
import ai.dokus.media.backend.withRemoteServices
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

fun Application.configureRouting() {
    logger.info("Configuring media routes...")

    routing {
        // Health check endpoint
        healthRoutes()

        // REST API routes (new)
        mediaRoutes()

        // RPC routes (legacy - will be deprecated)
        withRemoteServices()
    }

    logger.info("Routes configured for media service")
}
