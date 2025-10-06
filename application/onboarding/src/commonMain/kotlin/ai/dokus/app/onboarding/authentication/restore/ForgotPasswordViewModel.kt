package ai.dokus.app.app.onboarding.authentication.restore

import ai.dokus.app.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.app.repository.api.UnifiedApi
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class ForgotPasswordViewModel :
    BaseViewModel<ForgotPasswordViewModel.State>(State.Idle), KoinComponent {

    private val validateEmailUseCase: ValidateEmailUseCase by inject()
    private val api: UnifiedApi by inject()

    fun submit(emailValue: String) = scope.launch {
        mutableState.value = State.Loading

        if (!validateEmailUseCase(emailValue)) {
            mutableState.value = State.Error(PredictException.InvalidEmail)
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