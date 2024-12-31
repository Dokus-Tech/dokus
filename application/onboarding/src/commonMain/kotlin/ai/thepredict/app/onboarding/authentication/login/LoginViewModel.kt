package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.app.core.di
import ai.thepredict.app.core.extension.launchStreamScoped
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.User
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.repository.api.UnifiedApi
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import org.kodein.di.instance

internal class LoginViewModel : StateScreenModel<LoginViewModel.State>(State.Idle) {

    private val api: UnifiedApi by di.instance { screenModelScope }

    fun login(emailValue: String, passwordValue: String) {
        screenModelScope.launchStreamScoped {
            mutableState.value = State.Loading

            val existingUser = api.authenticate(emailValue, passwordValue)

            existingUser.getOrNull()?.also { user: User ->
                with(persistence) {
                    userId = user.id.toString()
                    email = user.email
                    password = user.password
                }

                mutableState.value = State.Authenticated

                return@launchStreamScoped
            }

            mutableState.value = State.Error(existingUser.asPredictException)
        }
    }

    sealed interface State {
        data object Idle : State

        data object Loading : State

        data object Authenticated : State

        data class Error(val exception: PredictException) : State
    }
}