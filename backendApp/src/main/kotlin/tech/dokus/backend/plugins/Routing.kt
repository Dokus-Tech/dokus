package tech.dokus.backend.plugins

import tech.dokus.foundation.backend.config.AppBaseConfig
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import tech.dokus.backend.routes.auth.configureAuthRoutes
import tech.dokus.backend.routes.cashflow.configureCashflowRoutes
import tech.dokus.backend.routes.common.configureCommonRoutes
import tech.dokus.backend.routes.contacts.configureContactsRoutes
import tech.dokus.backend.routes.payment.configurePaymentRoutes

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
