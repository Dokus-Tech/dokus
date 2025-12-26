package ai.dokus.backend.routes.banking

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("BankingRoutes")

fun Application.configureBankingRouting() {
    logger.info("Configuring routes...")

    routing {}

    logger.info("Routes configured: health checks")
}
