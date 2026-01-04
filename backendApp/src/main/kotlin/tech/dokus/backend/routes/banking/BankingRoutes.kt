package tech.dokus.backend.routes.banking

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("BankingRoutes")

fun Application.configureBankingRouting() {
    logger.info("Configuring routes...")

    routing {}

    logger.info("Routes configured: health checks")
}
