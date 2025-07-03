package ai.thepredict.app.onboarding.authentication.restore

import ai.thepredict.app.core.di
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class NewPasswordViewModel :
    StateScreenModel<NewPasswordViewModel.State>(State.Idle) {

    private val validatePasswordUseCase: ValidatePasswordUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun submit(password: String, passwordConfirmation: String) = screenModelScope.launch {
        mutableState.value = State.Loading

        if (!validatePasswordUseCase(password)) {
            mutableState.value = State.Error(PredictException.WeakPassword)
            return@launch
        }
//
//        persistence.user = api.authenticate(emailValue, passwordValue).getOrElse {
//            mutableState.value = State.Error(it.asPredictException)
//            return@launch
//        }

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