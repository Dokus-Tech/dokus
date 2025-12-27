package tech.dokus.backend.routes.banking

import ai.dokus.foundation.ktor.utils.loggerFor
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

private val logger = loggerFor("BankingRoutes")

fun Application.configureBankingRouting() {
    logger.info("Configuring routes...")

    routing {}

    logger.info("Routes configured: health checks")
}
