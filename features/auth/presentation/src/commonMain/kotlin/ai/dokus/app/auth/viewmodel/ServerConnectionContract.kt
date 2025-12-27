package ai.dokus.app.auth.viewmodel

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Server Connection screen.
 *
 * Flow:
 * 1. Input → User enters protocol, host, and port
 * 2. Validating → Server is being validated via API call
 * 3. Preview → Server validated, showing details for confirmation
 * 4. Connecting → Connection in progress after user confirmation
 * 5. Error → Display error with retry option
 *
 * Navigation flows:
 * - Successful connection → NavigateToLogin
 * - Reset to cloud → NavigateToLogin
 * - Back button → NavigateBack
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ServerConnectionState : MVIState, DokusState<Nothing> {
    val protocol: String
    val host: String
    val port: String

    /**
     * Initial state - user enters server details.
     */
    data class Input(
        override val protocol: String = "http",
        override val host: String = "",
        override val port: String = "8000",
        val hostError: String? = null,
        val portError: String? = null,
    ) : ServerConnectionState

    /**
     * Server is being validated.
     */
    data class Validating(
        override val protocol: String,
        override val host: String,
        override val port: String,
    ) : ServerConnectionState

    /**
     * Server validated, showing preview for confirmation.
     */
    data class Preview(
        override val protocol: String,
        override val host: String,
        override val port: String,
        val config: ServerConfig,
        val serverInfo: ServerInfo,
    ) : ServerConnectionState

    /**
     * Connection in progress after user confirmation.
     */
    data class Connecting(
        override val protocol: String,
        override val host: String,
        override val port: String,
        val config: ServerConfig,
        val serverInfo: ServerInfo,
    ) : ServerConnectionState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val protocol: String,
        override val host: String,
        override val port: String,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ServerConnectionState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ServerConnectionIntent : MVIIntent {
    /** User changed protocol selection (http/https) */
    data class UpdateProtocol(val value: String) : ServerConnectionIntent

    /** User typed in host field */
    data class UpdateHost(val value: String) : ServerConnectionIntent

    /** User typed in port field */
    data class UpdatePort(val value: String) : ServerConnectionIntent

    /** User clicked validate button */
    data object ValidateClicked : ServerConnectionIntent

    /** User confirmed connection after preview */
    data object ConfirmConnection : ServerConnectionIntent

    /** User cancelled preview */
    data object CancelPreview : ServerConnectionIntent

    /** User clicked reset to cloud */
    data object ResetToCloud : ServerConnectionIntent

    /** User clicked back button */
    data object BackClicked : ServerConnectionIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ServerConnectionAction : MVIAction {
    /** Navigate to login screen after successful connection */
    data object NavigateToLogin : ServerConnectionAction

    /** Navigate back to previous screen */
    data object NavigateBack : ServerConnectionAction
}
