package ai.dokus.reporting.backend.plugins

import ai.dokus.foundation.domain.rpc.ReportingApi
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.reporting.backend.routes.reportRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks, report routes, and RPC services.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        reportRoutes()

        // Register RPC APIs
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<ReportingApi> { get<ReportingApi>() }
        }
    }

    logger.info("Routes configured: health checks, report routes, and RPC services")
    logger.info("RPC APIs registered at /api")
}
