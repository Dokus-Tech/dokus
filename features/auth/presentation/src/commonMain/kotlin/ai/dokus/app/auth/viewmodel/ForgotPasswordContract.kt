package ai.dokus.app.auth.viewmodel

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Forgot Password screen.
 *
 * Flow:
 * 1. Idle → User enters email
 * 2. Submitting → Validating email and sending reset request
 * 3. Success → Navigate back (or show success message)
 * 4. Error → Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ForgotPasswordState : MVIState, DokusState<Unit> {
    val email: Email

    /**
     * Initial state - user enters email.
     */
    data class Idle(
        override val email: Email = Email(""),
    ) : ForgotPasswordState

    /**
     * Submitting password reset request.
     */
    data class Submitting(
        override val email: Email,
    ) : ForgotPasswordState

    /**
     * Password reset email sent successfully.
     */
    data class Success(
        override val email: Email,
    ) : ForgotPasswordState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val email: Email,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ForgotPasswordState, DokusState.Error<Unit>
}

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
