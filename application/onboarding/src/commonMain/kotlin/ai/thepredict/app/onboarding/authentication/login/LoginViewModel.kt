package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.repository.api.UnifiedApi
import ai.thepredict.repository.extensions.user
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import org.kodein.di.instance

internal class LoginViewModel : StateScreenModel<LoginViewModel.State>(State.Idle) {

    private val validateEmailUseCase: ValidateEmailUseCase by di.instance()
    private val validatePasswordUseCase: ValidatePasswordUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun login(emailValue: String, passwordValue: String) = screenModelScope.launchStreamScoped {
        mutableState.value = State.Loading

        if (!validateEmailUseCase(emailValue)) {
            mutableState.value = State.Error(PredictException.InvalidEmail)
            return@launchStreamScoped
        }
        if (!validatePasswordUseCase(passwordValue)) {
            mutableState.value = State.Error(PredictException.WeakPassword)
            return@launchStreamScoped
        }

        persistence.user = api.authenticate(emailValue, passwordValue).getOrElse {
            mutableState.value = State.Error(it.asPredictException)
            return@launchStreamScoped
        }

        mutableState.value = State.Authenticated
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data object Authenticated : State

        data class Error(val exception: PredictException) : State
    }

    sealed interface FieldsValidationState {
        data object Ok : FieldsValidationState
        data class Error(val exception: PredictException) : FieldsValidationState
    }
}