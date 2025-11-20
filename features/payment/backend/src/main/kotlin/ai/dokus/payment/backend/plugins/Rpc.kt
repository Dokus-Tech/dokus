package ai.dokus.payment.backend.plugins

import ai.dokus.foundation.ktor.auth.configureJwtAuth
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.koin.ktor.ext.get
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

    // Configure JWT authentication using Ktor's standard JWT plugin
    val jwtValidator = get<JwtValidator>()
    configureJwtAuth(jwtValidator)

    logger.info("KotlinX RPC configured with JWT authentication")
}
