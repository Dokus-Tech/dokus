package ai.dokus.app.onboarding.authentication.login

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.app.repository.extensions.authCredentials
import ai.dokus.app.repository.extensions.user
import ai.dokus.foundation.apispec.AuthApi
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.AuthCredentials
import ai.dokus.foundation.domain.model.JwtTokenDataSchema
import ai.dokus.foundation.domain.model.LoginRequest
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.platform.Logger
import ai.dokus.foundation.platform.persistence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class LoginViewModel : BaseViewModel<LoginViewModel.State>(State.Idle), KoinComponent {

    private val logger = Logger.forClass<LoginViewModel>()

    private val validateEmailUseCase: ValidateEmailUseCase by inject()
    private val validatePasswordUseCase: ValidatePasswordUseCase by inject()
    private val authApi: AuthApi by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun login(emailValue: Email, passwordValue: Password) = scope.launch {
        logger.d { "Login attempt started for email: ${emailValue.value.take(3)}***" }
        mutableState.value = State.Loading

        if (!validateEmailUseCase(emailValue)) {
            logger.w { "Login failed: invalid email format" }
            mutableState.value = State.Error(DokusException.InvalidEmail)
            return@launch
        }
        if (!validatePasswordUseCase(passwordValue)) {
            logger.w { "Login failed: weak password" }
            mutableState.value = State.Error(DokusException.WeakPassword)
            return@launch
        }

        val loginRequest = LoginRequest(emailValue, passwordValue)
        val jwtRaw = authApi.login(loginRequest).getOrElse {
            logger.e(it) { "Login API call failed" }
            mutableState.value = State.Error(it.asDokusException)
            return@launch
        }

        val jwtSchema = JwtTokenDataSchema.from(jwtRaw).getOrElse {
            logger.e(it) { "JWT parsing failed" }
            mutableState.value = State.Error(it.asDokusException)
            return@launch
        }

        persistence.authCredentials = AuthCredentials.from(jwtSchema, jwtRaw)
        persistence.user = User.from(jwtSchema)

        logger.i { "Login successful, navigating to workspaces" }
        mutableEffect.emit(Effect.NavigateToWorkspaces)
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateToWorkspaces : Effect
    }

    sealed interface FieldsValidationState {
        data object Ok : FieldsValidationState
        data class Error(val exception: DokusException) : FieldsValidationState
    }
}