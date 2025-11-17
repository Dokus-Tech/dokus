package ai.dokus.audit.backend.plugins

import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.services.AuditService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks and RPC services.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()

        // Register RPC service
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<AuditService> { get<AuditService>() }
        }
    }

    logger.info("Routes configured: health checks and RPC services")
    logger.info("AuditService registered at /api/rpc")
}
