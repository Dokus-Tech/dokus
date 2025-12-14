package ai.dokus.auth.backend.plugins

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.routes.serverInfoRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures health check and server info routes.
 * Note: REST API routes are configured separately via configureRoutes() in Application.kt
 */
fun Application.configureRouting() {
    val appConfig: AppBaseConfig by inject()

    logger.info("Configuring health and server info routes...")

    routing {
        healthRoutes()
        serverInfoRoutes(appConfig.serverInfo)
    }

    logger.info("Health and server info routes configured")
}
