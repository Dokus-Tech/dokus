package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.foundation.domain.config.ServerConfig
import tech.dokus.domain.exceptions.asDokusException
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce

internal typealias ServerConnectionCtx = PipelineContext<ServerConnectionState, ServerConnectionIntent, ServerConnectionAction>

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
        data class Params(
            val initialConfig: ServerConfig?
        )
    }

    private val logger = Logger.forClass<ServerConnectionContainer>()

    override val store: Store<ServerConnectionState, ServerConnectionIntent, ServerConnectionAction> =
        store(
            ServerConnectionState.Input(
                protocol = initialConfig?.protocol ?: "http",
                host = initialConfig?.host ?: "",
                port = (initialConfig?.port ?: 8000).toString()
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
            when (this) {
                is ServerConnectionState.Input -> copy(protocol = value)
                is ServerConnectionState.Error -> ServerConnectionState.Input(
                    protocol = value,
                    host = host,
                    port = port
                )
                else -> this
            }
        }
    }

    private suspend fun ServerConnectionCtx.handleUpdateHost(value: String) {
        updateState {
            when (this) {
                is ServerConnectionState.Input -> copy(host = value, hostError = null)
                is ServerConnectionState.Error -> ServerConnectionState.Input(
                    protocol = protocol,
                    host = value,
                    port = port
                )
                else -> this
            }
        }
    }

    private suspend fun ServerConnectionCtx.handleUpdatePort(value: String) {
        updateState {
            when (this) {
                is ServerConnectionState.Input -> copy(port = value, portError = null)
                is ServerConnectionState.Error -> ServerConnectionState.Input(
                    protocol = protocol,
                    host = host,
                    port = value
                )
                else -> this
            }
        }
    }

    private suspend fun ServerConnectionCtx.handleValidate() {
        withState<ServerConnectionState.Input, _> {
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
                ServerConnectionState.Validating(
                    protocol = currentProtocol,
                    host = currentHost,
                    port = currentPort
                )
            }

            logger.d { "Validating server: ${config.baseUrl}" }

            // Validate server
            connectToServerUseCase.validateAndPreview(config).fold(
                onSuccess = { serverInfo ->
                    logger.i { "Server validated: ${serverInfo.name}" }
                    updateState {
                        ServerConnectionState.Preview(
                            protocol = currentProtocol,
                            host = currentHost,
                            port = currentPort,
                            config = config,
                            serverInfo = serverInfo
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Server validation failed" }
                    updateState {
                        ServerConnectionState.Error(
                            protocol = currentProtocol,
                            host = currentHost,
                            port = currentPort,
                            exception = error.asDokusException,
                            retryHandler = { intent(ServerConnectionIntent.ValidateClicked) }
                        )
                    }
                }
            )
        }
    }

    private suspend fun ServerConnectionCtx.handleConfirmConnection() {
        withState<ServerConnectionState.Preview, _> {
            val currentProtocol = protocol
            val currentHost = host
            val currentPort = port
            val currentConfig = config
            val currentServerInfo = serverInfo

            // Transition to connecting state
            updateState {
                ServerConnectionState.Connecting(
                    protocol = currentProtocol,
                    host = currentHost,
                    port = currentPort,
                    config = currentConfig,
                    serverInfo = currentServerInfo
                )
            }

            logger.i { "Confirming connection to: ${currentServerInfo.name}" }

            // Confirm and connect
            connectToServerUseCase.confirmAndConnect(currentConfig, currentServerInfo)

            // Navigate to login
            action(ServerConnectionAction.NavigateToLogin)
        }
    }

    private suspend fun ServerConnectionCtx.handleCancelPreview() {
        withState<ServerConnectionState.Preview, _> {
            updateState {
                ServerConnectionState.Input(
                    protocol = protocol,
                    host = host,
                    port = port
                )
            }
        }
    }

    private suspend fun ServerConnectionCtx.handleResetToCloud() {
        // Capture current state values and transition to validating
        var currentProtocol = ""
        var currentHost = ""
        var currentPort = ""

        updateState {
            currentProtocol = protocol
            currentHost = host
            currentPort = port

            ServerConnectionState.Validating(
                protocol = currentProtocol,
                host = currentHost,
                port = currentPort
            )
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
                    ServerConnectionState.Error(
                        protocol = currentProtocol,
                        host = currentHost,
                        port = currentPort,
                        exception = error.asDokusException,
                        retryHandler = { intent(ServerConnectionIntent.ResetToCloud) }
                    )
                }
            }
        )
    }

    private fun validateHost(host: String): String? {
        return when {
            host.isBlank() -> "Host is required"
            host.contains(" ") -> "Host cannot contain spaces"
            else -> null
        }
    }

    private fun validatePort(port: String): String? {
        val portNum = port.toIntOrNull()
        return when {
            port.isBlank() -> "Port is required"
            portNum == null -> "Port must be a number"
            portNum !in 1..65535 -> "Port must be between 1 and 65535"
            else -> null
        }
    }
}
