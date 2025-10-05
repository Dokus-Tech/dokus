package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.apispec.AuthApi
import ai.thepredict.apispec.UserApi
import ai.thepredict.app.core.di
import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.app.platform.persistence
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.exceptions.asPredictException
import ai.thepredict.domain.model.AuthCredentials
import ai.thepredict.domain.model.CreateUserRequest
import ai.thepredict.domain.model.JwtTokenDataSchema
import ai.thepredict.domain.model.LoginRequest
import ai.thepredict.domain.model.User
import ai.thepredict.domain.usecases.CreateNewUserUseCase
import ai.thepredict.repository.extensions.authCredentials
import ai.thepredict.repository.extensions.user
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.kodein.di.instance

internal class RegisterViewModel : BaseViewModel<RegisterViewModel.State>(State.Loading) {

    private val createNewUserUseCase: CreateNewUserUseCase by di.instance()

    private val authApi: AuthApi by di.instance()
    private val userApi: UserApi by di.instance()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun createUser(newEmail: String, newPassword: String, firstName: String, lastName: String) {
        scope.launch {
            createNewUserUseCase(
                firstName = firstName,
                lastName = lastName,
                email = newEmail,
                password = newPassword
            ).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            val request = CreateUserRequest(
                firstName = firstName,
                lastName = lastName,
                email = newEmail,
                password = newPassword
            )
            val createdUser = userApi.createUser(request).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@getOrElse
            }

            val loginRequest = LoginRequest(email = newEmail, password = newPassword)
            val jwtRaw = authApi.login(loginRequest).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            val jwtSchema = JwtTokenDataSchema.from(jwtRaw).getOrElse {
                mutableState.value = State.Error(it.asPredictException)
                return@launch
            }

            persistence.authCredentials = AuthCredentials.from(jwtSchema, jwtRaw)
            persistence.user = User.from(jwtSchema)

            mutableEffect.emit(Effect.NavigateToRegistrationConfirmation)
        }
    }

    sealed interface State {
        data object Loading : State

        data class Error(val exception: PredictException) : State
    }

    sealed interface Effect {
        data object NavigateToRegistrationConfirmation : Effect
    }
}