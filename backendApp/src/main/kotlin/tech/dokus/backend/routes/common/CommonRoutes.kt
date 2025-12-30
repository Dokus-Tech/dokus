package tech.dokus.backend.routes.common

import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.routes.healthRoutes
import tech.dokus.foundation.backend.routes.serverInfoRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/**
 * Configures common routes shared across the application.
 *
 * Routes registered:
 * - /health, /health/live, /health/ready - Health check endpoints
 * - /server-info - Server information endpoint
 */
fun Application.configureCommonRoutes(appConfig: AppBaseConfig) {
    routing {
        healthRoutes()
        serverInfoRoutes(appConfig.serverInfo)
    }
}
