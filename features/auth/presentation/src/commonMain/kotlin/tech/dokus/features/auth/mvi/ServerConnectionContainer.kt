package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isIdle
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal typealias ServerConnectionCtx =
    PipelineContext<ServerConnectionState, ServerConnectionIntent, ServerConnectionAction>

/**
 * Container for Server Connection screen using FlowMVI.
 * Manages the server connection flow from input to connection.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ServerConnectionContainer(
    initialConfig: ServerConfig?,
    private val connectToServerUseCase: ConnectToServerUseCase,
) : Container<ServerConnectionState, ServerConnectionIntent, ServerConnectionAction> {

    companion object {
        private const val DefaultPort = 8000
        private const val MinPort = 1
        private const val MaxPort = 65535

        data class Params(
            val initialConfig: ServerConfig?
        )
    }

    private val logger = Logger.forClass<ServerConnectionContainer>()

    override val store: Store<ServerConnectionState, ServerConnectionIntent, ServerConnectionAction> =
        store(
            ServerConnectionState(
                protocol = initialConfig?.protocol ?: "http",
                host = initialConfig?.host ?: "",
                port = (initialConfig?.port ?: DefaultPort).toString()
            )
        ) {
            reduce { intent ->
                when (intent) {
                    is ServerConnectionIntent.UpdateProtocol -> handleUpdateProtocol(intent.value)
                    is ServerConnectionIntent.UpdateHost -> handleUpdateHost(intent.value)
                    is ServerConnectionIntent.UpdatePort -> handleUpdatePort(intent.value)
                    is ServerConnectionIntent.ValidateClicked -> handleValidate()
                    is ServerConnectionIntent.ConfirmConnection -> handleConfirmConnection()
                    is ServerConnectionIntent.CancelPreview -> handleCancelPreview()
                    is ServerConnectionIntent.ResetToCloud -> handleResetToCloud()
                    is ServerConnectionIntent.BackClicked -> action(ServerConnectionAction.NavigateBack)
                }
            }
        }

    private suspend fun ServerConnectionCtx.handleUpdateProtocol(value: String) {
        updateState {
            copy(
                protocol = value,
                validation = DokusState.idle(),
                hostError = null,
                portError = null,
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleUpdateHost(value: String) {
        updateState {
            copy(
                host = value,
                hostError = null,
                validation = DokusState.idle(),
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleUpdatePort(value: String) {
        updateState {
            copy(
                port = value,
                portError = null,
                validation = DokusState.idle(),
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleValidate() {
        withState {
            // Only allow validation from idle/error state
            if (!validation.isIdle() && !validation.isSuccess()) return@withState

            // Validate host
            val hostError = validateHost(host)
            val portError = validatePort(port)

            if (hostError != null || portError != null) {
                updateState {
                    copy(hostError = hostError, portError = portError)
                }
                return@withState
            }

            val currentProtocol = protocol
            val currentHost = host
            val currentPort = port

            // Create config
            val config = ServerConfig.fromManualEntry(
                host = currentHost,
                port = currentPort.toInt(),
                protocol = currentProtocol
            )

            // Transition to validating state
            updateState {
                copy(validation = DokusState.loading())
            }

            logger.d { "Validating server: ${config.baseUrl}" }

            // Validate server
            connectToServerUseCase.validateAndPreview(config).fold(
                onSuccess = { serverInfo ->
                    logger.i { "Server validated: ${serverInfo.name}" }
                    updateState {
                        copy(
                            validation = DokusState.success(
                                ServerValidation(config = config, serverInfo = serverInfo)
                            )
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Server validation failed" }
                    updateState {
                        copy(
                            validation = DokusState.error(
                                exception = error.asDokusException,
                                retryHandler = { intent(ServerConnectionIntent.ValidateClicked) }
                            )
                        )
                    }
                }
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleConfirmConnection() {
        withState {
            val validationState = validation
            if (!validationState.isSuccess()) return@withState

            val currentConfig = validationState.data.config
            val currentServerInfo = validationState.data.serverInfo

            // Transition to connecting state
            updateState {
                copy(isConnecting = true)
            }

            logger.i { "Confirming connection to: ${currentServerInfo.name}" }

            // Confirm and connect
            connectToServerUseCase.confirmAndConnect(currentConfig, currentServerInfo)

            // Navigate to login
            action(ServerConnectionAction.NavigateToLogin)
        }
    }

    private suspend fun ServerConnectionCtx.handleCancelPreview() {
        updateState {
            copy(
                validation = DokusState.idle(),
                isConnecting = false,
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleResetToCloud() {
        // Transition to loading
        updateState {
            copy(validation = DokusState.loading())
        }

        logger.i { "Resetting to cloud server" }

        // Reset to cloud
        connectToServerUseCase.resetToCloud().fold(
            onSuccess = {
                logger.i { "Reset to cloud successful" }
                action(ServerConnectionAction.NavigateToLogin)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to reset to cloud" }
                updateState {
                    copy(
                        validation = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ServerConnectionIntent.ResetToCloud) }
                        )
                    )
                }
            }
        )
    }

    private fun validateHost(host: String): DokusException? {
        return when {
            host.isBlank() -> DokusException.Validation.ServerHostRequired
            host.contains(" ") -> DokusException.Validation.ServerHostNoSpaces
            else -> null
        }
    }

    private fun validatePort(port: String): DokusException? {
        val portNum = port.toIntOrNull()
        return when {
            port.isBlank() -> DokusException.Validation.ServerPortRequired
            portNum == null -> DokusException.Validation.ServerPortInvalidNumber
            portNum !in MinPort..MaxPort -> DokusException.Validation.ServerPortOutOfRange
            else -> null
        }
    }
}
