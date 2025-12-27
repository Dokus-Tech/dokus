package ai.dokus.app.auth.viewmodel

import tech.dokus.domain.Password
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for New Password screen.
 *
 * Flow:
 * 1. Idle -> User enters password and confirmation
 * 2. Submitting -> Validating password and setting new password
 * 3. Success -> Navigate back (or show success message)
 * 4. Error -> Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface NewPasswordState : MVIState, DokusState<Unit> {
    val password: Password
    val passwordConfirmation: Password

    /**
     * Initial state - user enters password.
     */
    data class Idle(
        override val password: Password = Password(""),
        override val passwordConfirmation: Password = Password(""),
    ) : NewPasswordState

    /**
     * Submitting new password.
     */
    data class Submitting(
        override val password: Password,
        override val passwordConfirmation: Password,
    ) : NewPasswordState

    /**
     * Password set successfully.
     */
    data class Success(
        override val password: Password,
        override val passwordConfirmation: Password,
    ) : NewPasswordState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val password: Password,
        override val passwordConfirmation: Password,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : NewPasswordState, DokusState.Error<Unit>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface NewPasswordIntent : MVIIntent {
    /** User typed in password field */
    data class UpdatePassword(val value: Password) : NewPasswordIntent

    /** User typed in password confirmation field */
    data class UpdatePasswordConfirmation(val value: Password) : NewPasswordIntent

    /** User clicked submit button */
    data object SubmitClicked : NewPasswordIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface NewPasswordAction : MVIAction {
    /** Navigate back after successful password set */
    data object NavigateBack : NewPasswordAction
}
