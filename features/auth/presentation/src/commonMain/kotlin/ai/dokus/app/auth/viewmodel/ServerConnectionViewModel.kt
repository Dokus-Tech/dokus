package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.config.ServerConfig
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.config.ServerInfo
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for the server connection screen.
 *
 * Handles:
 * - Manual server entry (protocol, host, port)
 * - Server validation via API call
 * - Server connection confirmation
 * - Reset to cloud server
 *
 * The flow is split into two steps:
 * 1. Input/Validate: User enters details, clicks validate, server is checked
 * 2. Preview/Confirm: User reviews server info, confirms connection
 */
class ServerConnectionViewModel(
    initialConfig: ServerConfig? = null
) : BaseViewModel<ServerConnectionViewModel.State>(
    State(
        protocol = initialConfig?.protocol ?: "http",
        host = initialConfig?.host ?: "",
        port = (initialConfig?.port ?: 8000).toString()
    )
), KoinComponent {

    private val logger = Logger.forClass<ServerConnectionViewModel>()
    private val connectToServerUseCase: ConnectToServerUseCase by inject()
    private val serverConfigManager: ServerConfigManager by inject()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Input)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect = _effect.asSharedFlow()

    val currentServer: StateFlow<ServerConfig> = serverConfigManager.currentServer

    /**
     * Update the protocol selection (http/https).
     */
    fun onProtocolChange(protocol: String) {
        mutableState.value = state.value.copy(protocol = protocol)
    }

    /**
     * Update the host/IP address.
     */
    fun onHostChange(host: String) {
        mutableState.value = state.value.copy(
            host = host,
            hostError = null
        )
    }

    /**
     * Update the port number.
     */
    fun onPortChange(port: String) {
        mutableState.value = state.value.copy(
            port = port,
            portError = null
        )
    }

    /**
     * Validate the server connection.
     *
     * Creates a [ServerConfig] from the current input and calls the server's
     * /api/v1/server/info endpoint. On success, transitions to Preview state.
     */
    fun validateServer() {
        val currentState = state.value

        // Validate input
        val hostError = validateHost(currentState.host)
        val portError = validatePort(currentState.port)

        if (hostError != null || portError != null) {
            mutableState.value = currentState.copy(
                hostError = hostError,
                portError = portError
            )
            return
        }

        val config = ServerConfig.fromManualEntry(
            host = currentState.host,
            port = currentState.port.toInt(),
            protocol = currentState.protocol
        )

        scope.launch {
            _screenState.value = ScreenState.Validating
            logger.d { "Validating server: ${config.baseUrl}" }

            connectToServerUseCase.validateAndPreview(config).fold(
                onSuccess = { serverInfo ->
                    logger.i { "Server validated: ${serverInfo.name}" }
                    _screenState.value = ScreenState.Preview(config, serverInfo)
                },
                onFailure = { error ->
                    logger.e(error) { "Server validation failed" }
                    _screenState.value = ScreenState.Error(error)
                }
            )
        }
    }

    /**
     * Confirm connection to the previewed server.
     *
     * Clears local data and saves the new server configuration.
     * Navigates to login screen on completion.
     */
    fun confirmConnection() {
        val previewState = _screenState.value as? ScreenState.Preview ?: return

        scope.launch {
            _screenState.value = ScreenState.Connecting
            logger.i { "Confirming connection to: ${previewState.serverInfo.name}" }

            connectToServerUseCase.confirmAndConnect(previewState.config, previewState.serverInfo)

            _screenState.value = ScreenState.Connected
            _effect.emit(Effect.NavigateToLogin)
        }
    }

    /**
     * Cancel the preview and return to input state.
     */
    fun cancelPreview() {
        _screenState.value = ScreenState.Input
    }

    /**
     * Reset to the default Dokus Cloud server.
     */
    fun resetToCloud() {
        scope.launch {
            _screenState.value = ScreenState.Validating
            logger.i { "Resetting to cloud server" }

            connectToServerUseCase.resetToCloud().fold(
                onSuccess = {
                    _screenState.value = ScreenState.Connected
                    _effect.emit(Effect.NavigateToLogin)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to reset to cloud" }
                    _screenState.value = ScreenState.Error(error)
                }
            )
        }
    }

    /**
     * Retry after an error.
     */
    fun retry() {
        _screenState.value = ScreenState.Input
    }

    /**
     * Navigate back.
     */
    fun navigateBack() {
        scope.launch {
            _effect.emit(Effect.NavigateBack)
        }
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

    /**
     * Input state holding the form values.
     */
    data class State(
        val protocol: String = "http",
        val host: String = "",
        val port: String = "8000",
        val hostError: String? = null,
        val portError: String? = null
    )

    /**
     * Screen state representing the current phase of the connection flow.
     */
    sealed interface ScreenState {
        /** User is entering server details */
        data object Input : ScreenState

        /** Server is being validated */
        data object Validating : ScreenState

        /** Server validated, showing preview for confirmation */
        data class Preview(val config: ServerConfig, val serverInfo: ServerInfo) : ScreenState

        /** Connection in progress */
        data object Connecting : ScreenState

        /** Successfully connected */
        data object Connected : ScreenState

        /** Error during validation or connection */
        data class Error(val error: Throwable) : ScreenState
    }

    /**
     * One-time effects for navigation.
     */
    sealed interface Effect {
        data object NavigateToLogin : Effect
        data object NavigateBack : Effect
    }
}
