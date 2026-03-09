package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.ChangePasswordUseCase
import tech.dokus.foundation.platform.Logger

private typealias ChangePasswordCtx = PipelineContext<ChangePasswordState, ChangePasswordIntent, ChangePasswordAction>

internal class ChangePasswordContainer(
    private val changePasswordUseCase: ChangePasswordUseCase
) : Container<ChangePasswordState, ChangePasswordIntent, ChangePasswordAction> {

    private val logger = Logger.forClass<ChangePasswordContainer>()

    override val store: Store<ChangePasswordState, ChangePasswordIntent, ChangePasswordAction> =
        store(ChangePasswordState.initial) {
            reduce { intent ->
                when (intent) {
                    is ChangePasswordIntent.UpdateCurrentPassword -> updateCurrentPassword(intent.value)
                    is ChangePasswordIntent.UpdateNewPassword -> updateNewPassword(intent.value)
                    is ChangePasswordIntent.UpdateConfirmPassword -> updateConfirmPassword(intent.value)
                    ChangePasswordIntent.SubmitClicked -> submit()
                    ChangePasswordIntent.BackClicked -> action(ChangePasswordAction.NavigateBack)
                }
            }
        }

    private suspend fun ChangePasswordCtx.updateCurrentPassword(value: Password) {
        updateState {
            if (isSubmitting) this else copy(currentPassword = value, error = null)
        }
    }

    private suspend fun ChangePasswordCtx.updateNewPassword(value: Password) {
        updateState {
            if (isSubmitting) this else copy(newPassword = value, error = null)
        }
    }

    private suspend fun ChangePasswordCtx.updateConfirmPassword(value: Password) {
        updateState {
            if (isSubmitting) this else copy(confirmPassword = value, error = null)
        }
    }

    private suspend fun ChangePasswordCtx.submit() {
        var currentPassword = Password("")
        var newPassword = Password("")
        var confirmPassword = Password("")

        updateState {
            currentPassword = this.currentPassword
            newPassword = this.newPassword
            confirmPassword = this.confirmPassword
            copy(isSubmitting = true, error = null)
        }

        runCatching {
            currentPassword.validOrThrows
            newPassword.validOrThrows
            if (newPassword.value != confirmPassword.value) {
                throw DokusException.Validation.PasswordDoNotMatch
            }
        }.fold(
            onSuccess = {
                changePasswordUseCase(currentPassword, newPassword).fold(
                    onSuccess = {
                        logger.i { "Password changed successfully" }
                        action(ChangePasswordAction.NavigateBack)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to change password" }
                        updateState {
                            copy(isSubmitting = false, error = error.asDokusException)
                        }
                    }
                )
            },
            onFailure = { error ->
                updateState {
                    copy(isSubmitting = false, error = error.asDokusException)
                }
            }
        )
    }
}
