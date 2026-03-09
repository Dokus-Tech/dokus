package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.ResetPasswordUseCase
import tech.dokus.foundation.platform.Logger

internal typealias NewPasswordCtx = PipelineContext<NewPasswordState, NewPasswordIntent, NewPasswordAction>

/**
 * Container for New Password screen using FlowMVI.
 * Manages the password set flow from entry to success.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class NewPasswordContainer(
    private val resetToken: String,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : Container<NewPasswordState, NewPasswordIntent, NewPasswordAction> {

    private val logger = Logger.forClass<NewPasswordContainer>()

    override val store: Store<NewPasswordState, NewPasswordIntent, NewPasswordAction> =
        store(NewPasswordState.initial) {
            reduce { intent ->
                when (intent) {
                    is NewPasswordIntent.UpdatePassword -> handleUpdatePassword(intent.value)
                    is NewPasswordIntent.UpdatePasswordConfirmation -> handleUpdatePasswordConfirmation(intent.value)
                    is NewPasswordIntent.SubmitClicked -> handleSubmit()
                }
            }
        }

    private suspend fun NewPasswordCtx.handleUpdatePassword(value: Password) {
        updateState {
            if (isSubmitting) this else copy(password = value, error = null)
        }
    }

    private suspend fun NewPasswordCtx.handleUpdatePasswordConfirmation(value: Password) {
        updateState {
            if (isSubmitting) this else copy(passwordConfirmation = value, error = null)
        }
    }

    private suspend fun NewPasswordCtx.handleSubmit() {
        // Capture values during state transition
        var password: Password = Password("")
        var passwordConfirmation: Password = Password("")

        // Transition to submitting state and capture values
        updateState {
            password = this.password
            passwordConfirmation = this.passwordConfirmation
            copy(isSubmitting = true, error = null)
        }

        logger.d { "New password submission started" }

        // Validate password
        runCatching { password.validOrThrows }.fold(
            onSuccess = {
                // Check if passwords match
                if (password.value != passwordConfirmation.value) {
                    logger.w { "Password confirmation does not match" }
                    updateState {
                        copy(
                            isSubmitting = false,
                            error = DokusException.Validation.PasswordDoNotMatch,
                        )
                    }
                    return
                }

                if (resetToken.isBlank()) {
                    updateState {
                        copy(
                            isSubmitting = false,
                            error = DokusException.PasswordResetTokenInvalid(),
                        )
                    }
                    return
                }

                resetPasswordUseCase(resetToken, password).fold(
                    onSuccess = {
                        logger.i { "Password reset completed successfully" }
                        updateState {
                            copy(isSubmitting = false, isSuccess = true)
                        }
                        action(NewPasswordAction.NavigateBack)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Password reset failed" }
                        updateState {
                            copy(
                                isSubmitting = false,
                                error = error.asDokusException,
                            )
                        }
                    }
                )
            },
            onFailure = { error ->
                logger.e(error) { "New password validation failed" }
                updateState {
                    copy(
                        isSubmitting = false,
                        error = error.asDokusException,
                    )
                }
            }
        )
    }
}
