package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Email
import tech.dokus.domain.exceptions.DokusException

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class ForgotPasswordState(
    val email: Email = Email(""),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: DokusException? = null,
) : MVIState

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ForgotPasswordIntent : MVIIntent {
    /** User typed in email field */
    data class UpdateEmail(val value: Email) : ForgotPasswordIntent

    /** User clicked submit button */
    data object SubmitClicked : ForgotPasswordIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ForgotPasswordAction : MVIAction {
    /** Navigate back after successful password reset request */
    data object NavigateBack : ForgotPasswordAction
}
