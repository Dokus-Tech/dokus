package ai.dokus.cashflow.backend.plugins

import ai.dokus.foundation.ktor.auth.ServiceAuthPlugin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Rpc")

/**
 * Configures KotlinX RPC with authentication support.
 */
fun Application.configureRpc() {
    logger.info("Installing KotlinX RPC plugin...")

    // Install KotlinX RPC plugin
    install(Krpc)

    // Install service auth plugin (validates JWT locally)
    install(ServiceAuthPlugin)

    logger.info("KotlinX RPC configured with local JWT authentication")
}
