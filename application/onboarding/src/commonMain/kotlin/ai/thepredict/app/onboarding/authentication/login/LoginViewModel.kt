package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.apispec.AuthApi
import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.app.platform.Logger
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.JwtTokenDataSchema
import ai.thepredict.domain.model.LoginRequest
import ai.thepredict.domain.model.User
import ai.thepredict.domain.model.AuthCredentials
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.repository.extensions.authCredentials
import ai.thepredict.repository.extensions.user
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

    fun login(emailValue: String, passwordValue: String) = scope.launch {
        logger.d { "Login attempt started for email: ${emailValue.take(3)}***" }
        mutableState.value = State.Loading

        if (!validateEmailUseCase(emailValue)) {
            logger.w { "Login failed: invalid email format" }
            mutableState.value = State.Error(PredictException.InvalidEmail)
            return@launch
        }
        if (!validatePasswordUseCase(passwordValue)) {
            logger.w { "Login failed: weak password" }
            mutableState.value = State.Error(PredictException.WeakPassword)
            return@launch
        }

        val loginRequest = LoginRequest(emailValue, passwordValue)
        val jwtRaw = authApi.login(loginRequest).getOrElse {
            logger.e(it) { "Login API call failed" }
            mutableState.value = State.Error(it.asPredictException)
            return@launch
        }

        val jwtSchema = JwtTokenDataSchema.from(jwtRaw).getOrElse {
            logger.e(it) { "JWT parsing failed" }
            mutableState.value = State.Error(it.asPredictException)
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

        data class Error(val exception: PredictException) : State
    }

    sealed interface Effect {
        data object NavigateToWorkspaces : Effect
    }

    sealed interface FieldsValidationState {
        data object Ok : FieldsValidationState
        data class Error(val exception: PredictException) : FieldsValidationState
    }
}