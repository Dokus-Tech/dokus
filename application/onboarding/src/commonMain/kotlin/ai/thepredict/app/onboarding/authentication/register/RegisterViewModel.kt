package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.NewUser
import ai.thepredict.domain.User
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import org.kodein.di.instance

internal class RegisterViewModel : StateScreenModel<RegisterViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun createUser(newEmail: String, newPassword: String, name: String) {
        if (newEmail.isEmpty() && newPassword.isEmpty() && name.isEmpty()) return

        screenModelScope.launchStreamScoped {
            val newUser = NewUser(name = name, _email = newEmail, password = newPassword)
            val createdUser = api.createUser(newUser)

            createdUser.getOrNull()?.also { user: User ->
                with(persistence) {
                    userId = user.id.toString()
                    email = user.email
                    password = user.password
                }

                mutableState.value = State.Loaded(user)

                return@launchStreamScoped
            }

            mutableState.value = State.Error(createdUser.asPredictException)
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val user: User) : State

        data class Error(val exception: PredictException) : State
    }
}