package ai.dokus.app.auth.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.CreateNewUserUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class RegisterViewModel : BaseViewModel<RegisterViewModel.State>(State.Loading),
    KoinComponent {

    private val createNewUserUseCase: CreateNewUserUseCase by inject()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun createUser(newEmail: Email, newPassword: Password, firstName: Name, lastName: Name) {
        scope.launch {
        }
    }

    sealed interface State {
        data object Loading : State

        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateToRegistrationConfirmation : Effect
    }
}