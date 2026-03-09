@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Email
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.RequestPasswordResetUseCase
import tech.dokus.foundation.platform.Logger

// Number of characters to show in email preview for logging (privacy)
private const val EmailPreviewLength = 3

internal typealias ForgotPasswordCtx = PipelineContext<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction>

/**
 * Container for Forgot Password screen using FlowMVI.
 * Manages the password reset flow from email entry to success.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ForgotPasswordContainer(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase
) : Container<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction> {

    private val logger = Logger.forClass<ForgotPasswordContainer>()

    override val store: Store<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction> =
        store(ForgotPasswordState()) {
            reduce { intent ->
                when (intent) {
                    is ForgotPasswordIntent.UpdateEmail -> handleUpdateEmail(intent.value)
                    is ForgotPasswordIntent.SubmitClicked -> handleSubmit()
                }
            }
        }

    private suspend fun ForgotPasswordCtx.handleUpdateEmail(value: Email) {
        updateState {
            copy(email = value, error = null)
        }
    }

    private suspend fun ForgotPasswordCtx.handleSubmit() {
        // Capture values during state transition
        var email: Email = Email("")

        // Transition to submitting state and capture values
        updateState {
            email = this.email
            copy(isSubmitting = true, error = null)
        }

        logger.d { "Password reset attempt started for email: ${email.value.take(EmailPreviewLength)}***" }

        // Validate email
        runCatching { email.validOrThrows }.fold(
            onSuccess = {
                requestPasswordResetUseCase(email).fold(
                    onSuccess = {
                        logger.i { "Password reset requested successfully" }
                        updateState {
                            copy(isSubmitting = false, isSuccess = true)
                        }
                        action(ForgotPasswordAction.NavigateBack)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Password reset request failed" }
                        updateState {
                            copy(isSubmitting = false, error = error.asDokusException)
                        }
                    }
                )
            },
            onFailure = { error ->
                logger.e(error) { "Password reset validation failed" }
                updateState {
                    copy(isSubmitting = false, error = error.asDokusException)
                }
            }
        )
    }
}
