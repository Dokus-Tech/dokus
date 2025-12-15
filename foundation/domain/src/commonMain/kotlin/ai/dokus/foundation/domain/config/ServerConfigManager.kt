package ai.dokus.foundation.domain.config

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the current server configuration.
 *
 * Responsible for:
 * - Tracking the current server connection
 * - Persisting server configuration across app restarts
 * - Notifying observers of server changes
 *
 * The manager should be initialized during app startup to restore the previously
 * selected server. Use [setServer] to switch to a new server (this should only
 * be called after validating the server with [ValidateServerUseCase]).
 */
interface ServerConfigManager {
    /**
     * Current active server configuration.
     *
     * Emits the persisted server config on app start, or [ServerConfig.Cloud] if
     * no server has been previously configured.
     */
    val currentServer: StateFlow<ServerConfig>

    /**
     * Whether the current server is the official Dokus cloud server.
     *
     * Derived from [currentServer.isCloud].
     */
    val isCloudServer: StateFlow<Boolean>

    /**
     * Set the active server configuration.
     *
     * This will:
     * 1. Persist the configuration to local storage
     * 2. Update [currentServer] flow
     *
     * Note: Call this only after validating the server. The server switch will
     * take effect immediately for new HTTP requests.
     *
     * @param config The server configuration to set
     */
    suspend fun setServer(config: ServerConfig)

    /**
     * Reset to the default cloud server.
     *
     * Convenience method equivalent to `setServer(ServerConfig.Cloud)`.
     */
    suspend fun resetToCloud()

    /**
     * Load the persisted server configuration.
     *
     * Should be called during app initialization, before any network requests.
     * If no server was previously configured, defaults to [ServerConfig.Cloud].
     */
    suspend fun initialize()
}
