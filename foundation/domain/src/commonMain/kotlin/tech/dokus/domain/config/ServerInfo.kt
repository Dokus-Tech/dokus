package tech.dokus.domain.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information about a Dokus server, returned by the /api/v1/server/info endpoint.
 *
 * Used to validate that a server is a compatible Dokus instance and to display
 * server details to users before they confirm a connection.
 *
 * @property name Human-readable name of the server (e.g., "Dokus Cloud", "My Home Server")
 * @property version Server software version (e.g., "1.0.0")
 * @property environment Deployment environment ("cloud" or "self-hosted")
 * @property status Current server status
 * @property features List of enabled features/modules on this server
 */
@Serializable
data class ServerInfo(
    val name: String,
    val version: String,
    val environment: String,
    val status: ServerStatus,
    val features: List<String> = emptyList()
) {
    /**
     * Check if the server is available for connections.
     */
    val isAvailable: Boolean
        get() = status == ServerStatus.UP

    /**
     * Check if the server is in maintenance mode.
     */
    val isInMaintenance: Boolean
        get() = status == ServerStatus.MAINTENANCE

    /**
     * Check if this is the official cloud server.
     */
    val isCloud: Boolean
        get() = environment == "cloud"
}

/**
 * Server operational status.
 */
@Serializable
enum class ServerStatus {
    @SerialName("UP")
    UP,

    @SerialName("DOWN")
    DOWN,

    @SerialName("MAINTENANCE")
    MAINTENANCE
}
