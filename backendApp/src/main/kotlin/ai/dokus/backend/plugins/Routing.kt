package ai.dokus.backend.plugins

import ai.dokus.backend.routes.auth.configureAuthRoutes
import ai.dokus.backend.routes.cashflow.configureCashflowRoutes
import ai.dokus.backend.routes.contacts.contactRoutes
import ai.dokus.backend.routes.payment.paymentRoutes
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.routes.serverInfoRoutes
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
