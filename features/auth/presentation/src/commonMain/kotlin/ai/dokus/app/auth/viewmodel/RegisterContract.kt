package ai.dokus.app.auth.viewmodel

import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Register screen.
 *
 * Flow:
 * 1. Idle → User enters email, password, first name, and last name
 * 2. Registering → Creating account and authenticating with backend
 * 3. Success → Navigate to Home or WorkspaceSelect
 * 4. Error → Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface RegisterState : MVIState, DokusState<Unit> {
    val email: Email
    val password: Password
    val firstName: Name
    val lastName: Name

    /**
     * Initial state - user enters credentials and name.
     */
    data class Idle(
        override val email: Email = Email(""),
        override val password: Password = Password(""),
        override val firstName: Name = Name(""),
        override val lastName: Name = Name(""),
    ) : RegisterState

    /**
     * Registering and authenticating with backend.
     */
    data class Registering(
        override val email: Email,
        override val password: Password,
        override val firstName: Name,
        override val lastName: Name,
    ) : RegisterState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val email: Email,
        override val password: Password,
        override val firstName: Name,
        override val lastName: Name,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : RegisterState, DokusState.Error<Unit>
}

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
