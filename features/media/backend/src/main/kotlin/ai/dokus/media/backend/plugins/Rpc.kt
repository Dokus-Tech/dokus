package ai.dokus.media.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.rpc.krpc.ktor.server.Krpc
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Rpc")

fun Application.configureRpc() {
    logger.info("Installing KotlinX RPC plugin for media service...")
    install(Krpc)
    logger.info("KotlinX RPC configured")
}
