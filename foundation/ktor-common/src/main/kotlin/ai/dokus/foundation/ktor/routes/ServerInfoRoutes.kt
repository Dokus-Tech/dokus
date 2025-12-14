package ai.dokus.foundation.ktor.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server info endpoint for client connection validation.
 *
 * This endpoint is used by mobile/desktop clients to:
 * 1. Validate that the server is a Dokus instance
 * 2. Get server metadata (name, version, status)
 * 3. Check available features
 *
 * The endpoint is public (no authentication required) to allow clients
 * to validate servers before attempting login.
 */
fun Routing.serverInfoRoutes(
    serviceName: String = System.getenv("SERVICE_NAME") ?: "Dokus Server"
) {
    get("/api/v1/server/info") {
        val serverInfo = ServerInfoResponse(
            name = System.getenv("SERVER_NAME") ?: serviceName,
            version = System.getenv("SERVER_VERSION") ?: "1.0.0",
            environment = System.getenv("ENVIRONMENT") ?: "self-hosted",
            status = ServerInfoStatus.UP,
            features = getEnabledFeatures()
        )
        call.respond(HttpStatusCode.OK, serverInfo)
    }
}

/**
 * Response for the /api/v1/server/info endpoint.
 *
 * This format matches the ServerInfo class in the frontend domain module.
 */
@Serializable
data class ServerInfoResponse(
    val name: String,
    val version: String,
    val environment: String,
    val status: ServerInfoStatus,
    val features: List<String>
)

/**
 * Server status for the info endpoint.
 *
 * Note: This is separate from the health check ServerStatus to avoid confusion.
 * This enum maps to the client's ServerStatus enum.
 */
@Serializable
enum class ServerInfoStatus {
    @SerialName("UP")
    UP,

    @SerialName("DOWN")
    DOWN,

    @SerialName("MAINTENANCE")
    MAINTENANCE
}

/**
 * Get list of enabled features based on environment or configuration.
 */
private fun getEnabledFeatures(): List<String> {
    // Core features that are always available
    val coreFeatures = listOf(
        "auth",
        "invoicing",
        "expenses",
        "contacts"
    )

    // Optional features based on environment
    val optionalFeatures = mutableListOf<String>()

    if (System.getenv("BANKING_ENABLED")?.toBoolean() != false) {
        optionalFeatures.add("banking")
    }

    if (System.getenv("PAYMENTS_ENABLED")?.toBoolean() != false) {
        optionalFeatures.add("payments")
    }

    return coreFeatures + optionalFeatures
}
