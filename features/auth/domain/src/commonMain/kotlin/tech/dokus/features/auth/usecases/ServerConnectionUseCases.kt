package tech.dokus.features.auth.usecases

import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
import tech.dokus.domain.config.ServerValidationResult

/**
 * Use case for validating server configurations.
 */
interface ValidateServerUseCase {
    suspend operator fun invoke(config: ServerConfig): ServerValidationResult
}

/**
 * Use case for connecting to a server.
 */
interface ConnectToServerUseCase {
    suspend fun validateAndPreview(config: ServerConfig): Result<ServerInfo>

    suspend fun confirmAndConnect(config: ServerConfig, serverInfo: ServerInfo)

    suspend fun resetToCloud(): Result<ServerInfo>
}
