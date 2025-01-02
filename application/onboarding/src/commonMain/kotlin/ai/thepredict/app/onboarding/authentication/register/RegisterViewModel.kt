package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.app.platform.persistence
import ai.thepredict.data.User
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import org.kodein.di.instance

internal class RegisterViewModel : StateScreenModel<RegisterViewModel.State>(State.Loading) {

    private val createNewUserUseCase: CreateNewUserUseCase by di.instance()
    private val api: UnifiedApi by di.instance { screenModelScope }

    fun createUser(newEmail: String, newPassword: String, name: String) {
        screenModelScope.launchStreamScoped {
            val newUser = createNewUserUseCase(
                name = name,
                email = newEmail,
                password = newPassword
            ).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launchStreamScoped
            }

            val user = api.createUser(newUser).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launchStreamScoped
            }

            with(persistence) {
                userId = user.id.toString()
                email = user.email
                password = user.password
            }

            mutableState.value = State.Loaded(user)
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val user: User) : State

        data class Error(val exception: PredictException) : State
    }
}