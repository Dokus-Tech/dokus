package ai.thepredict.app.onboarding.register

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.domain.Contact
import ai.thepredict.domain.NewUser
import ai.thepredict.domain.User
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.kodein.di.instance

internal class RegisterViewModel : StateScreenModel<RegisterViewModel.State>(State.Loading) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun createUser(email: String, password: String, name: String) {
        if (email.isEmpty() && password.isEmpty() && name.isEmpty()) return

        screenModelScope.launchStreamScoped {
            val newUser = NewUser(name = name, _email = email, password = password)
            val createdUser = api.createUser(newUser)

            if (createdUser.getOrNull() != null) {
                mutableState.value = State.Loaded(createdUser.getOrThrow())
            } else {
                mutableState.value =
                    State.Error(createdUser.exceptionOrNull() ?: RuntimeException())
            }
        }
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(val user: User) : State

        data class Error(val throwable: Throwable) : State
    }
}