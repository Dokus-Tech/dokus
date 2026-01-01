package tech.dokus.features.auth.mvi

import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias NewPasswordCtx = PipelineContext<NewPasswordState, NewPasswordIntent, NewPasswordAction>

/**
 * Container for New Password screen using FlowMVI.
 * Manages the password set flow from entry to success.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class NewPasswordContainer : Container<NewPasswordState, NewPasswordIntent, NewPasswordAction> {

    private val logger = Logger.forClass<NewPasswordContainer>()

    override val store: Store<NewPasswordState, NewPasswordIntent, NewPasswordAction> =
        store(NewPasswordState.Idle()) {
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
            when (this) {
                is NewPasswordState.Idle -> copy(password = value)
                is NewPasswordState.Error -> NewPasswordState.Idle(
                    password = value,
                    passwordConfirmation = passwordConfirmation
                )
                is NewPasswordState.Success -> NewPasswordState.Idle(
                    password = value,
                    passwordConfirmation = passwordConfirmation
                )
                is NewPasswordState.Submitting -> this
            }
        }
    }

    private suspend fun NewPasswordCtx.handleUpdatePasswordConfirmation(value: Password) {
        updateState {
            when (this) {
                is NewPasswordState.Idle -> copy(passwordConfirmation = value)
                is NewPasswordState.Error -> NewPasswordState.Idle(
                    password = password,
                    passwordConfirmation = value
                )
                is NewPasswordState.Success -> NewPasswordState.Idle(
                    password = password,
                    passwordConfirmation = value
                )
                is NewPasswordState.Submitting -> this
            }
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
            NewPasswordState.Submitting(
                password = this.password,
                passwordConfirmation = this.passwordConfirmation
            )
        }

        logger.d { "New password submission started" }

        // Validate password
        runCatching { password.validOrThrows }.fold(
            onSuccess = {
                // Check if passwords match
                if (password.value != passwordConfirmation.value) {
                    logger.w { "Password confirmation does not match" }
                    updateState {
                        NewPasswordState.Error(
                            password = password,
                            passwordConfirmation = passwordConfirmation,
                            exception = DokusException.Validation.PasswordDoNotMatch,
                            retryHandler = { intent(NewPasswordIntent.SubmitClicked) }
                        )
                    }
                    return
                }

                // TODO: Call password set API when implemented
                // For now, just transition to success state after validation
                logger.i { "New password validated, transitioning to success" }
                updateState {
                    NewPasswordState.Success(
                        password = password,
                        passwordConfirmation = passwordConfirmation
                    )
                }
                action(NewPasswordAction.NavigateBack)
            },
            onFailure = { error ->
                logger.e(error) { "New password validation failed" }
                updateState {
                    NewPasswordState.Error(
                        password = password,
                        passwordConfirmation = passwordConfirmation,
                        exception = error.asDokusException,
                        retryHandler = { intent(NewPasswordIntent.SubmitClicked) }
                    )
                }
            }
        )
    }
}
