package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException

@Immutable
data class ChangePasswordState(
    val currentPassword: Password = Password(""),
    val newPassword: Password = Password(""),
    val confirmPassword: Password = Password(""),
    val isSubmitting: Boolean = false,
    val error: DokusException? = null
) : MVIState {
    companion object {
        val initial by lazy { ChangePasswordState() }
    }
}

@Immutable
sealed interface ChangePasswordIntent : MVIIntent {
    data class UpdateCurrentPassword(val value: Password) : ChangePasswordIntent
    data class UpdateNewPassword(val value: Password) : ChangePasswordIntent
    data class UpdateConfirmPassword(val value: Password) : ChangePasswordIntent
    data object SubmitClicked : ChangePasswordIntent
    data object BackClicked : ChangePasswordIntent
}

@Immutable
sealed interface ChangePasswordAction : MVIAction {
    data object NavigateBack : ChangePasswordAction
}
