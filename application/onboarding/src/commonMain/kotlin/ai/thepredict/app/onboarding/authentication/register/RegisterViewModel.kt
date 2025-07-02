package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.app.core.di
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.old.User
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class RegisterViewModel : StateScreenModel<RegisterViewModel.State>(State.Loading) {

    private val createNewUserUseCase: CreateNewUserUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun createUser(newEmail: String, newPassword: String, name: String) {
        screenModelScope.launch {
            val newUser = createNewUserUseCase(
                name = name,
                email = newEmail,
                password = newPassword
            ).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val user: User) : State

        data class Error(val exception: PredictException) : State
    }
}