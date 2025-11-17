package ai.dokus.payment.backend.plugins

import ai.dokus.foundation.domain.rpc.PaymentApi
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.payment.backend.routes.paymentRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

/**
 * Configures all application routes including health checks, payment routes, and RPC services.
 */
fun Application.configureRouting() {
    logger.info("Configuring routes...")

    routing {
        healthRoutes()
        paymentRoutes()

        // Register RPC APIs
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<PaymentApi> { get<PaymentApi>() }
        }
    }

    logger.info("Routes configured: health checks, payment routes, and RPC services")
    logger.info("RPC APIs registered at /api")
}
