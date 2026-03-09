package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException

/**
 * Contract for New Password screen.
 *
 * Flat data class with form fields always visible, plus boolean/nullable
 * flags for transient states (submitting, success, error).
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class NewPasswordState(
    val password: Password = Password(""),
    val passwordConfirmation: Password = Password(""),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: DokusException? = null,
) : MVIState

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
