package ai.dokus.backend.plugins

import ai.dokus.backend.routes.auth.configureAuthRoutes
import ai.dokus.backend.routes.cashflow.configureCashflowRoutes
import ai.dokus.backend.routes.common.configureCommonRoutes
import ai.dokus.backend.routes.contacts.configureContactsRoutes
import ai.dokus.backend.routes.payment.configurePaymentRoutes
import ai.dokus.foundation.ktor.config.AppBaseConfig
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all routes for the modular monolith server.
 *
 * All route configuration follows a consistent pattern:
 * - Each feature exposes a configure*Routes() Application extension function
 * - Routes are organized by feature domain
 */
fun Application.configureRouting(appConfig: AppBaseConfig) {
    logger.info("Configuring routes...")

    // Common routes (health, server info)
    configureCommonRoutes(appConfig)

    // Feature routes
    configureAuthRoutes()
    configureCashflowRoutes()
    configureContactsRoutes()
    configurePaymentRoutes()

    // Banking routes disabled until implemented
    // configureBankingRoutes()

    logger.info("Routes configured")
}
