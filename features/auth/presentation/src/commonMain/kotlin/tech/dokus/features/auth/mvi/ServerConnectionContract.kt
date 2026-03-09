package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Server Connection screen.
 *
 * Flow:
 * 1. Input → User enters protocol, host, and port (validation idle)
 * 2. Validating → Server is being validated via API call (validation loading)
 * 3. Preview → Server validated, showing details for confirmation (validation success, !isConnecting)
 * 4. Connecting → Connection in progress after user confirmation (validation success, isConnecting)
 * 5. Error → Display error with retry option (validation error)
 *
 * Navigation flows:
 * - Successful connection → NavigateToLogin
 * - Reset to cloud → NavigateToLogin
 * - Back button → NavigateBack
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * Validated server details including config and server metadata.
 */
data class ServerValidation(
    val config: ServerConfig,
    val serverInfo: ServerInfo,
)

/**
 * Flat state for the server connection screen.
 *
 * Uses [DokusState] for the validation lifecycle:
 * - idle → user is entering details
 * - loading → validating the server
 * - success → server validated, preview available
 * - error → validation or connection failed
 *
 * [isConnecting] distinguishes preview (false) from active connection (true)
 * when validation is successful.
 */
@Immutable
data class ServerConnectionState(
    val protocol: String = "http",
    val host: String = "",
    val port: String = "8000",
    val hostError: DokusException? = null,
    val portError: DokusException? = null,
    val validation: DokusState<ServerValidation> = DokusState.idle(),
    val isConnecting: Boolean = false,
) : MVIState {
    companion object {
        val initial by lazy { ServerConnectionState() }
    }
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
