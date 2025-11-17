package ai.dokus.auth.backend.plugins

import ai.dokus.foundation.ktor.middleware.RpcAuthPlugin
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.util.AttributeKey
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Rpc")

/**
 * Configures KotlinX RPC with authentication support.
 * Installs the KRPC plugin and RPC authentication middleware.
 */
fun Application.configureRpc() {
    logger.info("Installing KotlinX RPC plugin...")

    // Install KotlinX RPC plugin
    install(Krpc)

    // Install RPC authentication plugin
    val jwtValidator = get<JwtValidator>()
    attributes.put(AttributeKey("JwtValidator"), jwtValidator)
    install(RpcAuthPlugin)

    logger.info("KotlinX RPC configured with authentication")
}
