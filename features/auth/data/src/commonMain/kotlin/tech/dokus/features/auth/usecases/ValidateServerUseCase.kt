@file:Suppress(
    "TooGenericExceptionCaught" // Network validation can fail in various ways
)

package tech.dokus.features.auth.usecases

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
import tech.dokus.domain.config.ServerStatus
import tech.dokus.domain.config.ServerValidationResult
import tech.dokus.domain.utils.json
import tech.dokus.foundation.platform.Logger

/** Timeout for the entire HTTP request in milliseconds */
private const val RequestTimeoutMs = 10_000L

/** Timeout for establishing connection in milliseconds */
private const val ConnectTimeoutMs = 5_000L

/**
 * Validates that a server is a compatible Dokus instance.
 *
 * Makes a request to the server's /api/v1/server/info endpoint to:
 * 1. Verify the server is reachable
 * 2. Verify it's a Dokus server (correct response format)
 * 3. Check server status (UP, DOWN, MAINTENANCE)
 * 4. Retrieve server metadata (name, version, features)
 */
class ValidateServerUseCase {
    private val logger = Logger.forClass<ValidateServerUseCase>()

    /**
     * Validate a server configuration.
     *
     * @param config The server configuration to validate
     * @return [ServerValidationResult.Valid] with server info if validation succeeds,
     *         [ServerValidationResult.Invalid] with reason if validation fails
     */
    suspend operator fun invoke(config: ServerConfig): ServerValidationResult {
        logger.d { "Validating server: ${config.baseUrl}" }

        // Create a temporary HTTP client for validation
        val client = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = RequestTimeoutMs
                connectTimeoutMillis = ConnectTimeoutMs
            }
        }

        return try {
            val response = client.get("${config.baseUrl}/api/v1/server/info")

            if (response.status.isSuccess()) {
                val serverInfo = response.body<ServerInfo>()
                logger.i { "Server validated: ${serverInfo.name} v${serverInfo.version}" }

                when (serverInfo.status) {
                    ServerStatus.MAINTENANCE -> {
                        logger.w { "Server is in maintenance mode" }
                        ServerValidationResult.Invalid(ServerValidationResult.InvalidReason.MAINTENANCE)
                    }

                    ServerStatus.DOWN -> {
                        logger.w { "Server reports status as DOWN" }
                        ServerValidationResult.Invalid(ServerValidationResult.InvalidReason.UNREACHABLE)
                    }

                    ServerStatus.UP, ServerStatus.WARN, ServerStatus.UNKNOWN -> {
                        ServerValidationResult.Valid(serverInfo)
                    }
                }
            } else {
                logger.w { "Server responded with status: ${response.status}" }
                ServerValidationResult.Invalid(ServerValidationResult.InvalidReason.NOT_DOKUS_SERVER)
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to validate server" }
            ServerValidationResult.Invalid(ServerValidationResult.InvalidReason.UNREACHABLE)
        } finally {
            client.close()
        }
    }
}
