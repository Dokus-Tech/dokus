package ai.dokus.app.auth.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.app.repository.api.UnifiedApi
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class NewPasswordViewModel :
    BaseViewModel<NewPasswordViewModel.State>(State.Idle), KoinComponent {

    private val validatePasswordUseCase: ValidatePasswordUseCase by inject()
    private val api: UnifiedApi by inject()

    fun submit(password: Password, passwordConfirmation: Password) = scope.launch {
        mutableState.value = State.Loading

        if (!validatePasswordUseCase(password)) {
            mutableState.value = State.Error(DokusException.WeakPassword)
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

        data class Error(val exception: DokusException) : State
    }

    sealed interface FieldsValidationState {
        data object Ok : FieldsValidationState
        data class Error(val exception: DokusException) : FieldsValidationState
    }
}