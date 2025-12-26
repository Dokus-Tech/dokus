package ai.dokus.server.backend.plugins

import ai.dokus.auth.backend.routes.configureRoutes as configureAuthRoutes
import ai.dokus.cashflow.backend.routes.configureCashflowRoutes
import ai.dokus.contacts.backend.routes.contactRoutes
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.routes.serverInfoRoutes
import ai.dokus.payment.backend.routes.paymentRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all routes for the modular monolith server.
 */
fun Application.configureRouting(appConfig: AppBaseConfig) {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        serverInfoRoutes(appConfig.serverInfo)
        contactRoutes()
        paymentRoutes()
    }

    // Feature route registries (they add their own routing blocks)
    configureAuthRoutes()
    configureCashflowRoutes()

    logger.info("Routes configured")
}

