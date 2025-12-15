package ai.dokus.foundation.domain.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Provides dynamic endpoint configuration based on the current server selection.
 *
 * This provider bridges [ServerConfigManager] and the HTTP client configuration.
 * When the user switches servers, the endpoint configuration is automatically
 * updated and new HTTP clients will use the new server.
 *
 * Note: Existing HTTP client instances will NOT be updated. The factory pattern
 * ensures new clients are created with the current endpoint configuration.
 *
 * @property serverConfigManager The server configuration manager
 */
class DynamicDokusEndpointProvider(
    private val serverConfigManager: ServerConfigManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Current endpoint configuration derived from the active server.
     *
     * This flow emits a new [DynamicEndpoint] whenever the server configuration
     * changes. HTTP client factories should use this to configure new clients.
     */
    val currentEndpoint: StateFlow<DynamicEndpoint> = serverConfigManager.currentServer
        .map { DynamicEndpoint.fromServerConfig(it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = DynamicEndpoint.fromServerConfig(ServerConfig.Cloud)
        )
}

/**
 * Endpoint configuration for dynamic server connections.
 *
 * Unlike [DokusEndpoint] which uses compile-time BuildKonfig values,
 * [DynamicEndpoint] uses runtime values from the user's selected server.
 *
 * @property host Server hostname or IP address
 * @property port Server port number
 * @property protocol Connection protocol ("http" or "https")
 * @property pathPrefix API path prefix (always "/api/v1" for Dokus)
 */
data class DynamicEndpoint(
    val host: String,
    val port: Int,
    val protocol: String,
    val pathPrefix: String = "/api/v1"
) {
    companion object {
        /**
         * Create a [DynamicEndpoint] from a [ServerConfig].
         */
        fun fromServerConfig(config: ServerConfig): DynamicEndpoint {
            return DynamicEndpoint(
                host = config.host,
                port = config.port,
                protocol = config.protocol
            )
        }
    }
}
