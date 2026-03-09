package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCase
import tech.dokus.foundation.platform.Logger

// Number of characters to show in email preview for logging (privacy)
private const val EmailPreviewLength = 3

internal typealias RegisterCtx = PipelineContext<RegisterState, RegisterIntent, RegisterAction>

/**
 * Container for Register screen using FlowMVI.
 * Manages the registration flow from credential entry to navigation.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class RegisterContainer(
    private val registerAndLoginUseCase: RegisterAndLoginUseCase,
    private val tokenManager: TokenManager,
) : Container<RegisterState, RegisterIntent, RegisterAction> {

    private val logger = Logger.forClass<RegisterContainer>()

    override val store: Store<RegisterState, RegisterIntent, RegisterAction> =
        store(RegisterState()) {
            reduce { intent ->
                when (intent) {
                    is RegisterIntent.UpdateEmail -> updateState { copy(email = intent.value, error = null) }
                    is RegisterIntent.UpdatePassword -> updateState { copy(password = intent.value, error = null) }
                    is RegisterIntent.UpdateFirstName -> updateState { copy(firstName = intent.value, error = null) }
                    is RegisterIntent.UpdateLastName -> updateState { copy(lastName = intent.value, error = null) }
                    is RegisterIntent.RegisterClicked -> handleRegister()
                }
            }
        }

    private suspend fun RegisterCtx.handleRegister() {
        // Capture values during state transition
        var email = Email("")
        var password = Password("")
        var firstName = Name("")
        var lastName = Name("")

        // Transition to registering state and capture values
        updateState {
            email = this.email
            password = this.password
            firstName = this.firstName
            lastName = this.lastName
            copy(isRegistering = true, error = null)
        }

        logger.d { "Registration attempt started for email: ${email.value.take(EmailPreviewLength)}***" }

        // Attempt registration
        registerAndLoginUseCase(email, password, firstName, lastName).fold(
            onSuccess = {
                logger.i { "Registration successful, navigating to home" }
                updateState { copy(isRegistering = false) }
                if (tokenManager.getSelectedTenantId() == null) {
                    action(RegisterAction.NavigateToWorkspaceSelect)
                } else {
                    action(RegisterAction.NavigateToHome)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Registration failed" }
                updateState {
                    copy(isRegistering = false, error = error.asDokusException)
                }
            }
        )
    }
}
