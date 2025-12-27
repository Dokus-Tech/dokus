package ai.dokus.app.auth.viewmodel

import tech.dokus.domain.Email
import tech.dokus.domain.Password
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Login screen.
 *
 * Flow:
 * 1. Idle → User enters email and password
 * 2. Authenticating → Validating credentials with backend
 * 3. Success → Navigate to Home or WorkspaceSelect
 * 4. Error → Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface LoginState : MVIState, DokusState<Unit> {
    val email: Email
    val password: Password

    /**
     * Initial state - user enters credentials.
     */
    data class Idle(
        override val email: Email = Email(""),
        override val password: Password = Password(""),
    ) : LoginState

    /**
     * Authenticating with backend.
     */
    data class Authenticating(
        override val email: Email,
        override val password: Password,
    ) : LoginState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val email: Email,
        override val password: Password,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : LoginState, DokusState.Error<Unit>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface LoginIntent : MVIIntent {
    /** User typed in email field */
    data class UpdateEmail(val value: Email) : LoginIntent

    /** User typed in password field */
    data class UpdatePassword(val value: Password) : LoginIntent

    /** User clicked login button */
    data object LoginClicked : LoginIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface LoginAction : MVIAction {
    /** Navigate to home screen after successful login */
    data object NavigateToHome : LoginAction

    /** Navigate to workspace selection when no tenant is set */
    data object NavigateToWorkspaceSelect : LoginAction
}
