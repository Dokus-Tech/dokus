package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Name
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.User
import tech.dokus.foundation.app.state.DokusState

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class ProfileSettingsState(
    val user: DokusState<User> = DokusState.loading(),
    val isEditing: Boolean = false,
    val editFirstName: Name = Name.Empty,
    val editLastName: Name = Name.Empty,
    val isSaving: Boolean = false,
    val isResendingVerification: Boolean = false,
    val avatarState: AvatarState = AvatarState.Idle,
    val actionError: DokusException? = null,
) : MVIState {

    val hasChanges: Boolean
        get() {
            val currentUser = (user as? DokusState.Success)?.data ?: return false
            val currentFirstName = currentUser.firstName?.value ?: ""
            val currentLastName = currentUser.lastName?.value ?: ""
            return editFirstName.value != currentFirstName ||
                editLastName.value != currentLastName
        }

    val canSave: Boolean
        get() = isEditing && editFirstName.isValid && editLastName.isValid && hasChanges

    companion object {
        val initial by lazy { ProfileSettingsState() }
    }
}

@Immutable
sealed interface AvatarState {
    data object Idle : AvatarState
    data class Uploading(val progress: Float) : AvatarState
    data object Success : AvatarState
    data class Error(val error: DokusException) : AvatarState
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

    /** User selected a new avatar image */
    data class UploadAvatar(val imageBytes: ByteArray, val filename: String) : ProfileSettingsIntent

    /** Reset avatar state after transient feedback */
    data object ResetAvatarState : ProfileSettingsIntent

    /** User requested verification email resend */
    data object ResendVerificationClicked : ProfileSettingsIntent

    /** User wants to open change-password screen */
    data object ChangePasswordClicked : ProfileSettingsIntent

    /** User wants to open sessions management screen */
    data object MySessionsClicked : ProfileSettingsIntent

    /** User dismissed inline error banner */
    data object DismissActionError : ProfileSettingsIntent

    /** User clicked back button */
    data object BackClicked : ProfileSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ProfileSettingsAction : MVIAction {
    /** Navigate to change-password screen */
    data object NavigateToChangePassword : ProfileSettingsAction

    /** Navigate to sessions screen */
    data object NavigateToMySessions : ProfileSettingsAction

    /** Navigate back to previous screen */
    data object NavigateBack : ProfileSettingsAction
}
