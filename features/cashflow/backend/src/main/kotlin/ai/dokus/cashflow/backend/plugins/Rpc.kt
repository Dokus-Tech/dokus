package ai.dokus.cashflow.backend.plugins

import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.ktor.auth.createRpcAuthPlugin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Rpc")

/**
 * Configures KotlinX RPC with authentication support.
 */
fun Application.configureRpc() {
    logger.info("Installing KotlinX RPC plugin...")

    // Install KotlinX RPC plugin
    install(Krpc)

    // Install RPC authentication plugin
    val authValidationService = get<AuthValidationRemoteService>()
    install(createRpcAuthPlugin(authValidationService, "Cashflow"))

    logger.info("KotlinX RPC configured with authentication")
}
