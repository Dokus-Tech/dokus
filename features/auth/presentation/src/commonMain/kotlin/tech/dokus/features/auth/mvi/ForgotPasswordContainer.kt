package tech.dokus.features.auth.mvi

import tech.dokus.domain.Email
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias ForgotPasswordCtx = PipelineContext<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction>

/**
 * Container for Forgot Password screen using FlowMVI.
 * Manages the password reset flow from email entry to success.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ForgotPasswordContainer : Container<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction> {

    private val logger = Logger.forClass<ForgotPasswordContainer>()

    override val store: Store<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction> =
        store(ForgotPasswordState.Idle()) {
            reduce { intent ->
                when (intent) {
                    is ForgotPasswordIntent.UpdateEmail -> handleUpdateEmail(intent.value)
                    is ForgotPasswordIntent.SubmitClicked -> handleSubmit()
                }
            }
        }

    private suspend fun ForgotPasswordCtx.handleUpdateEmail(value: Email) {
        updateState {
            when (this) {
                is ForgotPasswordState.Idle -> copy(email = value)
                is ForgotPasswordState.Error -> ForgotPasswordState.Idle(email = value)
                is ForgotPasswordState.Success -> ForgotPasswordState.Idle(email = value)
                is ForgotPasswordState.Submitting -> this
            }
        }
    }

    private suspend fun ForgotPasswordCtx.handleSubmit() {
        // Capture values during state transition
        var email: Email = Email("")

        // Transition to submitting state and capture values
        updateState {
            email = this.email
            ForgotPasswordState.Submitting(email = this.email)
        }

        logger.d { "Password reset attempt started for email: ${email.value.take(3)}***" }

        // Validate email
        runCatching { email.validOrThrows }.fold(
            onSuccess = {
                // TODO: Call password reset API when implemented
                // For now, just transition to success state after validation
                logger.i { "Password reset email validated, transitioning to success" }
                updateState {
                    ForgotPasswordState.Success(email = email)
                }
                action(ForgotPasswordAction.NavigateBack)
            },
            onFailure = { error ->
                logger.e(error) { "Password reset validation failed" }
                updateState {
                    ForgotPasswordState.Error(
                        email = email,
                        exception = error.asDokusException,
                        retryHandler = { intent(ForgotPasswordIntent.SubmitClicked) }
                    )
                }
            }
        )
    }
}
