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
        store(ChangePasswordState.Idle()) {
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
            when (this) {
                is ChangePasswordState.Idle -> copy(currentPassword = value)
                is ChangePasswordState.Submitting -> this
                is ChangePasswordState.Error -> ChangePasswordState.Idle(
                    currentPassword = value,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            }
        }
    }

    private suspend fun ChangePasswordCtx.updateNewPassword(value: Password) {
        updateState {
            when (this) {
                is ChangePasswordState.Idle -> copy(newPassword = value)
                is ChangePasswordState.Submitting -> this
                is ChangePasswordState.Error -> ChangePasswordState.Idle(
                    currentPassword = currentPassword,
                    newPassword = value,
                    confirmPassword = confirmPassword
                )
            }
        }
    }

    private suspend fun ChangePasswordCtx.updateConfirmPassword(value: Password) {
        updateState {
            when (this) {
                is ChangePasswordState.Idle -> copy(confirmPassword = value)
                is ChangePasswordState.Submitting -> this
                is ChangePasswordState.Error -> ChangePasswordState.Idle(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = value
                )
            }
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
            ChangePasswordState.Submitting(
                currentPassword = this.currentPassword,
                newPassword = this.newPassword,
                confirmPassword = this.confirmPassword
            )
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
                        action(ChangePasswordAction.ShowSuccess)
                        action(ChangePasswordAction.NavigateBack)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to change password" }
                        updateState {
                            ChangePasswordState.Error(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword,
                                exception = error.asDokusException,
                                retryHandler = { intent(ChangePasswordIntent.SubmitClicked) }
                            )
                        }
                    }
                )
            },
            onFailure = { error ->
                updateState {
                    ChangePasswordState.Error(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        exception = error.asDokusException,
                        retryHandler = { intent(ChangePasswordIntent.SubmitClicked) }
                    )
                }
            }
        )
    }
}
