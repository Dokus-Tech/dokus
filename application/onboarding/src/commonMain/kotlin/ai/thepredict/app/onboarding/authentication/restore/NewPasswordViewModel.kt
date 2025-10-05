package ai.thepredict.app.onboarding.authentication.restore

import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.usecases.validators.ValidateEmailUseCase
import ai.thepredict.domain.usecases.validators.ValidatePasswordUseCase
import ai.thepredict.repository.api.UnifiedApi
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class NewPasswordViewModel :
    BaseViewModel<NewPasswordViewModel.State>(State.Idle), KoinComponent {

    private val validatePasswordUseCase: ValidatePasswordUseCase by inject()
    private val api: UnifiedApi by inject()

    fun submit(password: String, passwordConfirmation: String) = scope.launch {
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