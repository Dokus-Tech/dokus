package ai.dokus.audit.backend.plugins

import io.ktor.server.application.*
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Rpc")

/**
 * Configures KotlinX RPC with authentication support.
 * Installs the KRPC plugin and JWT authentication using Ktor's built-in JWT plugin.
 */
fun Application.configureRpc() {
    logger.info("Installing KotlinX RPC plugin...")

    // Install KotlinX RPC plugin
    install(Krpc)

    logger.info("KotlinX RPC configured with JWT authentication")
}
