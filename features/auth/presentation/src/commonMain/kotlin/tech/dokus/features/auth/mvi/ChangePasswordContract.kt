package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Password
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.state.DokusState

@Immutable
sealed interface ChangePasswordState : MVIState, DokusState<Unit> {
    val currentPassword: Password
    val newPassword: Password
    val confirmPassword: Password

    data class Idle(
        override val currentPassword: Password = Password(""),
        override val newPassword: Password = Password(""),
        override val confirmPassword: Password = Password("")
    ) : ChangePasswordState

    data class Submitting(
        override val currentPassword: Password,
        override val newPassword: Password,
        override val confirmPassword: Password
    ) : ChangePasswordState

    data class Error(
        override val currentPassword: Password,
        override val newPassword: Password,
        override val confirmPassword: Password,
        override val exception: DokusException,
        override val retryHandler: RetryHandler
    ) : ChangePasswordState, DokusState.Error<Unit>
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
    data object ShowSuccess : ChangePasswordAction
}
