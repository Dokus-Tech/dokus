@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Email
import tech.dokus.domain.Password
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.LoginUseCase
import tech.dokus.foundation.platform.Logger

// Number of characters to show in email preview for logging (privacy)
private const val EmailPreviewLength = 3

internal typealias LoginCtx = PipelineContext<LoginState, LoginIntent, LoginAction>

/**
 * Container for Login screen using FlowMVI.
 * Manages the authentication flow from credential entry to navigation.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class LoginContainer(
    private val loginUseCase: LoginUseCase,
    private val tokenManager: TokenManager,
) : Container<LoginState, LoginIntent, LoginAction> {

    private val logger = Logger.forClass<LoginContainer>()

    override val store: Store<LoginState, LoginIntent, LoginAction> =
        store(LoginState()) {
            reduce { intent ->
                when (intent) {
                    is LoginIntent.UpdateEmail -> handleUpdateEmail(intent.value)
                    is LoginIntent.UpdatePassword -> handleUpdatePassword(intent.value)
                    is LoginIntent.LoginClicked -> handleLogin()
                }
            }
        }

    private suspend fun LoginCtx.handleUpdateEmail(value: Email) {
        updateState { copy(email = value, error = null) }
    }

    private suspend fun LoginCtx.handleUpdatePassword(value: Password) {
        updateState { copy(password = value, error = null) }
    }

    private suspend fun LoginCtx.handleLogin() {
        // Capture values during state transition
        var email: Email = Email("")
        var password: Password = Password("")

        // Transition to authenticating state and capture values
        updateState {
            email = this.email
            password = this.password
            copy(isAuthenticating = true, error = null)
        }

        logger.d { "Login attempt started for email: ${email.value.take(EmailPreviewLength)}***" }

        // Attempt login
        loginUseCase(email, password).fold(
            onSuccess = {
                logger.i { "Login successful, navigating to home" }
                updateState { copy(isAuthenticating = false) }
                if (tokenManager.getSelectedTenantId() == null) {
                    action(LoginAction.NavigateToWorkspaceSelect)
                } else {
                    action(LoginAction.NavigateToHome)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Login failed" }
                updateState {
                    copy(
                        isAuthenticating = false,
                        error = error.asDokusException,
                    )
                }
            }
        )
    }
}
