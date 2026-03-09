package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Email
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class LoginState(
    val email: Email = Email(""),
    val password: Password = Password(""),
    val isAuthenticating: Boolean = false,
    val error: DokusException? = null,
) : MVIState

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
