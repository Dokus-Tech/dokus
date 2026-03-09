package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException

/**
 * Contract for Register screen.
 *
 * Flow:
 * 1. User enters email, password, first name, and last name
 * 2. Registering → Creating account and authenticating with backend
 * 3. Success → Navigate to Home or WorkspaceSelect
 * 4. Error → Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class RegisterState(
    val email: Email = Email(""),
    val password: Password = Password(""),
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val isRegistering: Boolean = false,
    val error: DokusException? = null,
) : MVIState

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface RegisterIntent : MVIIntent {
    /** User typed in email field */
    data class UpdateEmail(val value: Email) : RegisterIntent

    /** User typed in password field */
    data class UpdatePassword(val value: Password) : RegisterIntent

    /** User typed in first name field */
    data class UpdateFirstName(val value: Name) : RegisterIntent

    /** User typed in last name field */
    data class UpdateLastName(val value: Name) : RegisterIntent

    /** User clicked register button */
    data object RegisterClicked : RegisterIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface RegisterAction : MVIAction {
    /** Navigate to home screen after successful registration */
    data object NavigateToHome : RegisterAction

    /** Navigate to workspace selection when no tenant is set */
    data object NavigateToWorkspaceSelect : RegisterAction
}
