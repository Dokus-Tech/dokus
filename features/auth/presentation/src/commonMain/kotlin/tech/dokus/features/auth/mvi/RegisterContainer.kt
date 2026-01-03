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
        store(RegisterState.Idle()) {
            reduce { intent ->
                when (intent) {
                    is RegisterIntent.UpdateEmail -> handleUpdateEmail(intent.value)
                    is RegisterIntent.UpdatePassword -> handleUpdatePassword(intent.value)
                    is RegisterIntent.UpdateFirstName -> handleUpdateFirstName(intent.value)
                    is RegisterIntent.UpdateLastName -> handleUpdateLastName(intent.value)
                    is RegisterIntent.RegisterClicked -> handleRegister()
                }
            }
        }

    private suspend fun RegisterCtx.handleUpdateEmail(value: Email) {
        updateState {
            when (this) {
                is RegisterState.Idle -> copy(email = value)
                is RegisterState.Error -> RegisterState.Idle(
                    email = value,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                )
                is RegisterState.Registering -> this
            }
        }
    }

    private suspend fun RegisterCtx.handleUpdatePassword(value: Password) {
        updateState {
            when (this) {
                is RegisterState.Idle -> copy(password = value)
                is RegisterState.Error -> RegisterState.Idle(
                    email = email,
                    password = value,
                    firstName = firstName,
                    lastName = lastName
                )
                is RegisterState.Registering -> this
            }
        }
    }

    private suspend fun RegisterCtx.handleUpdateFirstName(value: Name) {
        updateState {
            when (this) {
                is RegisterState.Idle -> copy(firstName = value)
                is RegisterState.Error -> RegisterState.Idle(
                    email = email,
                    password = password,
                    firstName = value,
                    lastName = lastName
                )
                is RegisterState.Registering -> this
            }
        }
    }

    private suspend fun RegisterCtx.handleUpdateLastName(value: Name) {
        updateState {
            when (this) {
                is RegisterState.Idle -> copy(lastName = value)
                is RegisterState.Error -> RegisterState.Idle(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = value
                )
                is RegisterState.Registering -> this
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
            RegisterState.Registering(
                email = this.email,
                password = this.password,
                firstName = this.firstName,
                lastName = this.lastName
            )
        }

        logger.d { "Registration attempt started for email: ${email.value.take(EmailPreviewLength)}***" }

        // Attempt registration
        registerAndLoginUseCase(email, password, firstName, lastName).fold(
            onSuccess = {
                logger.i { "Registration successful, navigating to home" }
                val claims = tokenManager.getCurrentClaims()
                if (claims?.tenant == null) {
                    action(RegisterAction.NavigateToWorkspaceSelect)
                } else {
                    action(RegisterAction.NavigateToHome)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Registration failed" }
                updateState {
                    RegisterState.Error(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        exception = error.asDokusException,
                        retryHandler = { intent(RegisterIntent.RegisterClicked) }
                    )
                }
            }
        )
    }
}
