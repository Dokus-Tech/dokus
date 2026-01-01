package tech.dokus.features.auth.usecases

import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.config.ServerConnectionException
import tech.dokus.domain.config.ServerInfo
import tech.dokus.domain.config.ServerValidationResult
import tech.dokus.foundation.platform.Logger
import tech.dokus.foundation.platform.Persistence

/**
 * Handles the server connection flow.
 *
 * The connection flow is split into two steps:
 * 1. **Validate & Preview** ([validateAndPreview]): Validates the server and returns info
 *    for user confirmation. Does NOT switch servers yet.
 * 2. **Confirm & Connect** ([confirmAndConnect]): User confirmed, so clear all storage
 *    and switch to the new server.
 *
 * This two-step approach allows the UI to show server details to the user before
 * they commit to switching servers.
 */
class ConnectToServerUseCase(
    private val validateServer: ValidateServerUseCase,
    private val serverConfigManager: ServerConfigManager,
    private val tokenStorage: TokenStorage,
    private val persistence: Persistence
) {
    private val logger = Logger.forClass<ConnectToServerUseCase>()

    /**
     * Step 1: Validate server and return info for user confirmation.
     *
     * This method validates that the server is reachable and is a compatible Dokus
     * instance. If validation succeeds, it returns the server info for the UI to
     * display. The server switch does NOT happen yet - call [confirmAndConnect]
     * after the user confirms.
     *
     * @param config The server configuration to validate
     * @return [Result.success] with [ServerInfo] if validation succeeds,
     *         [Result.failure] with [ServerConnectionException] if validation fails
     */
    suspend fun validateAndPreview(config: ServerConfig): Result<ServerInfo> {
        logger.d { "Validating server for preview: ${config.baseUrl}" }

        return when (val validation = validateServer(config)) {
            is ServerValidationResult.Valid -> {
                logger.i { "Server validated successfully: ${validation.serverInfo.name}" }
                Result.success(validation.serverInfo)
            }
            is ServerValidationResult.Invalid -> {
                logger.w { "Server validation failed: ${validation.reason}" }
                val exception = when (validation.reason) {
                    ServerValidationResult.InvalidReason.UNREACHABLE ->
                        ServerConnectionException.unreachable()
                    ServerValidationResult.InvalidReason.NOT_DOKUS_SERVER ->
                        ServerConnectionException.notDokusServer()
                    ServerValidationResult.InvalidReason.INCOMPATIBLE_VERSION ->
                        ServerConnectionException.incompatibleVersion()
                    ServerValidationResult.InvalidReason.MAINTENANCE ->
                        ServerConnectionException.maintenance()
                    ServerValidationResult.InvalidReason.INVALID_URL ->
                        ServerConnectionException.invalidUrl()
                }
                Result.failure(exception)
            }
        }
    }

    /**
     * Step 2: User confirmed connection - clear all storage and switch.
     *
     * This method should be called after the user has reviewed the server info
     * returned by [validateAndPreview] and confirmed they want to connect.
     *
     * This will:
     * 1. Clear all authentication tokens
     * 2. Clear user persistence data (but preserve preferences like theme)
     * 3. Save the new server configuration with enriched info (name, version)
     *
     * After this method returns, the caller should navigate to the login screen.
     *
     * @param config The server configuration to switch to
     * @param serverInfo The server info returned from validation (used to enrich config)
     */
    suspend fun confirmAndConnect(config: ServerConfig, serverInfo: ServerInfo) {
        logger.i { "Confirming connection to server: ${serverInfo.name}" }

        // Step 1: Clear all authentication tokens
        logger.d { "Clearing authentication tokens" }
        tokenStorage.clearTokens()

        // Step 2: Clear user persistence data
        logger.d { "Clearing user persistence data" }
        persistence.clearUserData()

        // Step 3: Save new server config with enriched info from server
        val enrichedConfig = config.copy(
            name = serverInfo.name,
            version = serverInfo.version
        )
        logger.d { "Saving new server configuration" }
        serverConfigManager.setServer(enrichedConfig)

        logger.i { "Server switch complete. User should be navigated to login." }
    }

    /**
     * Reset to the default cloud server.
     *
     * Convenience method that switches back to Dokus Cloud. This will:
     * 1. Clear all authentication tokens
     * 2. Clear user persistence data
     * 3. Reset server config to cloud default
     *
     * @return [Result.success] if reset succeeds, [Result.failure] if cloud server
     *         is unreachable (which would be unusual)
     */
    suspend fun resetToCloud(): Result<ServerInfo> {
        logger.i { "Resetting to cloud server" }

        // Validate cloud server first
        val result = validateAndPreview(ServerConfig.Cloud)

        if (result.isSuccess) {
            confirmAndConnect(ServerConfig.Cloud, result.getOrThrow())
        }

        return result
    }
}
