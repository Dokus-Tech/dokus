package ai.dokus.cashflow.backend.plugins

import ai.dokus.cashflow.backend.rpc.AuthenticatedCashflowService
import ai.dokus.cashflow.backend.rpc.CashflowApiImpl
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.ktor.routes.healthRoutes
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

        // Register RPC API
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            // Register CashflowApi service with authentication wrapper
            registerService<CashflowApi> {
                AuthenticatedCashflowService(
                    delegate = get<CashflowApiImpl>()
                )
            }
        }
    }

    logger.info("Routes configured: health checks and RPC services")
    logger.info("RPC API available at /rpc")
}
