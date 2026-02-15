package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Name
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.User
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Profile Settings screen.
 *
 * Flow:
 * 1. Loading → Fetching user profile
 * 2. Viewing → Displaying user data
 * 3. Editing → User modifies profile fields
 * 4. Saving → Persisting changes to backend
 * 5. Error → Display error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ProfileSettingsState : MVIState, DokusState<User> {

    /**
     * Initial state - loading user profile.
     */
    data object Loading : ProfileSettingsState

    /**
     * Viewing user profile (not editing).
     */
    data class Viewing(
        val user: User,
        val isResendingVerification: Boolean = false,
    ) : ProfileSettingsState

    /**
     * Editing user profile.
     */
    data class Editing(
        val user: User,
        val editFirstName: Name,
        val editLastName: Name,
    ) : ProfileSettingsState {
        val hasChanges: Boolean
            get() {
                val currentFirstName = user.firstName?.value ?: ""
                val currentLastName = user.lastName?.value ?: ""
                return editFirstName.value != currentFirstName ||
                    editLastName.value != currentLastName
            }

        val canSave: Boolean
            get() = editFirstName.isValid && editLastName.isValid && hasChanges
    }

    /**
     * Saving profile changes.
     */
    data class Saving(
        val user: User,
        val editFirstName: Name,
        val editLastName: Name,
    ) : ProfileSettingsState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ProfileSettingsState, DokusState.Error<User>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ProfileSettingsIntent : MVIIntent {
    /** Load or reload profile data */
    data object LoadProfile : ProfileSettingsIntent

    /** User wants to start editing profile */
    data object StartEditing : ProfileSettingsIntent

    /** User cancelled editing */
    data object CancelEditing : ProfileSettingsIntent

    /** User typed in first name field */
    data class UpdateFirstName(val value: Name) : ProfileSettingsIntent

    /** User typed in last name field */
    data class UpdateLastName(val value: Name) : ProfileSettingsIntent

    /** User clicked save button */
    data object SaveClicked : ProfileSettingsIntent

    /** User requested verification email resend */
    data object ResendVerificationClicked : ProfileSettingsIntent

    /** User wants to open change-password screen */
    data object ChangePasswordClicked : ProfileSettingsIntent

    /** User wants to open sessions management screen */
    data object MySessionsClicked : ProfileSettingsIntent

    /** User clicked back button */
    data object BackClicked : ProfileSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ProfileSettingsAction : MVIAction {
    /** Profile saved successfully */
    data object ShowSaveSuccess : ProfileSettingsAction

    /** Profile save failed */
    data class ShowSaveError(val error: DokusException) : ProfileSettingsAction

    /** Verification email resent successfully */
    data object ShowVerificationEmailSent : ProfileSettingsAction

    /** Verification email resend failed */
    data class ShowVerificationEmailError(val error: DokusException) : ProfileSettingsAction

    /** Navigate to change-password screen */
    data object NavigateToChangePassword : ProfileSettingsAction

    /** Navigate to sessions screen */
    data object NavigateToMySessions : ProfileSettingsAction

    /** Navigate back to previous screen */
    data object NavigateBack : ProfileSettingsAction
}
