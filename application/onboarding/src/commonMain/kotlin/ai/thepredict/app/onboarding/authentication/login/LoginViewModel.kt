package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.apispec.AuthApi
import ai.thepredict.app.core.viewmodel.BaseViewModel
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

    private val validateEmailUseCase: ValidateEmailUseCase by inject()
    private val validatePasswordUseCase: ValidatePasswordUseCase by inject()
    private val authApi: AuthApi by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun login(emailValue: String, passwordValue: String) = scope.launch {
        mutableState.value = State.Loading

        if (!validateEmailUseCase(emailValue)) {
            mutableState.value = State.Error(PredictException.InvalidEmail)
            return@launch
        }
        if (!validatePasswordUseCase(passwordValue)) {
            mutableState.value = State.Error(PredictException.WeakPassword)
            return@launch
        }

        val loginRequest = LoginRequest(emailValue, passwordValue)
        val jwtRaw = authApi.login(loginRequest).getOrElse {
            mutableState.value = State.Error(it.asPredictException)
            return@launch
        }

        val jwtSchema = JwtTokenDataSchema.from(jwtRaw).getOrElse {
            mutableState.value = State.Error(it.asPredictException)
            return@launch
        }

        persistence.authCredentials = AuthCredentials.from(jwtSchema, jwtRaw)
        persistence.user = User.from(jwtSchema)

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